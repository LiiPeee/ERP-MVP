package br.com.erpcore.bootstrap;

import static br.com.erpcore.bootstrap.support.HttpApi.bearer;
import static br.com.erpcore.bootstrap.support.HttpApi.loginJson;
import static br.com.erpcore.bootstrap.support.HttpApi.registrationJson;
import static br.com.erpcore.bootstrap.support.HttpApi.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** O caminho completo pela API: do cadastro do cliente à decisão pelo link, com duas oficinas lado a lado. */
@ApplicationHttpTest
class QuoteApiIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final String ITEMS = """
            [{"description":"Troca de óleo","kind":"LABOR","quantity":"1","unitPrice":"80.00"},
             {"description":"Filtro de óleo","kind":"PART","quantity":"2","unitPrice":"25.50"}]""";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    @Value("${local.server.port}")
    private int port;

    /** Uma oficina com o dono já autenticado. */
    private final class Shop {
        final HttpApi api = new HttpApi(port);
        final String token;

        Shop() {
            String email = uniqueEmail("dono");
            assertThat(api.post("/api/auth/register", registrationJson(email, PASSWORD), Map.of()).status()).isEqualTo(201);
            token = api.post("/api/auth/login", loginJson(email, PASSWORD), Map.of()).json().get("accessToken").asString();
        }

        Reply get(String path) {
            return api.get(path, bearer(token));
        }

        Reply post(String path, String body) {
            return api.post(path, body, bearer(token));
        }

        Reply put(String path, String body) {
            return api.send("PUT", path, body, bearer(token));
        }

        String contact(String name) {
            Reply reply = post("/api/contacts", """
                    {"name":"%s","phone":"(11) 9%04d-%04d","email":null}""".formatted(name, SEQUENCE.incrementAndGet(), 4321));
            assertThat(reply.status()).as(reply.body()).isEqualTo(201);
            return reply.json().get("id").asString();
        }

        String vehicle(String contactId, String plate) {
            Reply reply = post("/api/contacts/" + contactId + "/vehicles", """
                    {"plate":"%s","brand":"Fiat","model":"Uno","year":2014,"odometerKm":120000}""".formatted(plate));
            assertThat(reply.status()).as(reply.body()).isEqualTo(201);
            return reply.json().get("id").asString();
        }

        String draft(String contactId, String vehicleId) {
            Reply reply = post("/api/quotes", """
                    {"contactId":"%s","vehicleId":"%s","items":%s,"note":"Revisão simples","validityDays":null}"""
                    .formatted(contactId, vehicleId, ITEMS));
            assertThat(reply.status()).as(reply.body()).isEqualTo(201);
            return reply.json().get("id").asString();
        }

        /** Cria cliente, veículo e orçamento já enviado; devolve o id e o token do link. */
        Sent sentQuote(String contactName, String plate) {
            String contactId = contact(contactName);
            String quoteId = draft(contactId, vehicle(contactId, plate));
            Reply sent = post("/api/quotes/" + quoteId + "/confirm", "{}");
            assertThat(sent.status()).as(sent.body()).isEqualTo(200);
            String url = sent.json().get("publicUrl").asString();
            return new Sent(quoteId, url.substring(url.lastIndexOf('/') + 1), url);
        }
    }

    private record Sent(String quoteId, String token, String url) {
    }

    private static String plate() {
        return "ABC1D" + (10 + SEQUENCE.incrementAndGet() % 90);
    }

    @Test
    void givenAnOwner_whenRegisteringAContactAndAVehicle_thenTheApiReturnsThemMasked() {
        Shop shop = new Shop();
        String contactId = shop.contact("Cliente Sintético");

        Reply vehicle = shop.post("/api/contacts/" + contactId + "/vehicles",
                """
                {"plate":"XYZ9K88","brand":"VW","model":"Gol","year":2010,"odometerKm":null}""");

        assertThat(vehicle.status()).isEqualTo(201);
        assertThat(vehicle.json().get("plateMasked").asString()).isEqualTo("****K88");
        assertThat(vehicle.body()).doesNotContain("XYZ9K88");
        Reply contacts = shop.get("/api/contacts");
        assertThat(contacts.status()).isEqualTo(200);
        assertThat(contacts.json().get(0).get("phoneMasked").asString()).matches("\\(11\\) \\*+-4321");
        assertThat(contacts.body()).doesNotContain("+55119");
        assertThat(shop.get("/api/contacts/" + contactId + "/vehicles").json().size()).isEqualTo(1);
    }

    @Test
    void givenAContactName_whenSearching_thenOnlyMatchesOfThisShopComeBackWithinTheCap() {
        Shop ana = new Shop();
        Shop beto = new Shop();
        ana.contact("Marcelo Sintético");
        beto.contact("Marcelo Alheio");

        Reply byName = ana.get("/api/contacts?name=marcelo");

        assertThat(byName.json().size()).isEqualTo(1);
        assertThat(byName.json().get(0).get("name").asString()).isEqualTo("Marcelo Sintético");
        assertThat(ana.get("/api/contacts?limit=100000").status()).isEqualTo(200);
        assertThat(ana.get("/api/contacts?limit=100000").json().size()).isLessThanOrEqualTo(50);
    }

    @Test
    void givenAPhoneAlreadyRegistered_whenRegisteringItAgain_thenItIsAConflictThatDoesNotEchoTheNumber() {
        Shop shop = new Shop();
        String body = """
                {"name":"Cliente Um","phone":"(11) 98888-7777","email":null}""";
        assertThat(shop.post("/api/contacts", body).status()).isEqualTo(201);

        Reply again = shop.post("/api/contacts", body);

        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("code").asString()).isEqualTo("PHONE_ALREADY_REGISTERED");
        assertThat(again.body()).doesNotContain("98888").doesNotContain("+5511");
    }

    @Test
    void givenAPlateAlreadyRegistered_whenRegisteringItAgain_thenItIsAConflict() {
        Shop shop = new Shop();
        String contactId = shop.contact("Cliente Placa");
        String plate = plate();
        shop.vehicle(contactId, plate);

        Reply again = shop.post("/api/contacts/" + contactId + "/vehicles",
                """
                {"plate":"%s","brand":"Fiat","model":"Uno","year":2014,"odometerKm":null}""".formatted(plate));

        assertThat(again.status()).isEqualTo(409);
        assertThat(again.json().get("code").asString()).isEqualTo("PLATE_ALREADY_REGISTERED");
        assertThat(again.body()).doesNotContain(plate);
    }

    @Test
    void givenInvalidData_whenCreating_thenTheErrorNamesTheFieldAndNotTheValue() {
        Shop shop = new Shop();

        Reply badPhone = shop.post("/api/contacts", """
                {"name":"Fulano","phone":"123","email":null}""");
        Reply badItems = shop.post("/api/quotes", """
                {"contactId":"%s","vehicleId":"%s","items":[{"description":"X","kind":"LABOR","quantity":"0","unitPrice":"1"}]}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(badPhone.status()).isEqualTo(422);
        assertThat(badPhone.json().get("field").asString()).isEqualTo("phone");
        assertThat(badPhone.body()).doesNotContain("123");
        assertThat(badItems.status()).isIn(404, 422);
    }

    @Test
    void givenAnOversizedField_whenCreating_thenItIsRefusedAtTheEdge() {
        Shop shop = new Shop();

        Reply reply = shop.post("/api/contacts", """
                {"name":"%s","phone":"(11) 97777-1111","email":null}""".formatted("a".repeat(5000)));

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json().get("code").asString()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void givenADraft_whenReadingTheQueue_thenItNeedsTheOwnerAndTheTotalIsComputedByTheServer() {
        Shop shop = new Shop();
        String contactId = shop.contact("Cliente Fila");
        String quoteId = shop.draft(contactId, shop.vehicle(contactId, plate()));

        Reply quote = shop.get("/api/quotes/" + quoteId);
        Reply queue = shop.get("/api/quotes");

        assertThat(quote.status()).isEqualTo(200);
        assertThat(quote.json().get("status").asString()).isEqualTo("DRAFT");
        assertThat(quote.json().get("total").asString()).isEqualTo("131.00");
        assertThat(quote.body()).doesNotContain("+5511");
        assertThat(queue.json().get("needsOwner").get(0).get("id").asString()).isEqualTo(quoteId);
        assertThat(queue.json().get("awaitingClient").size()).isZero();
    }

    @Test
    void givenADraft_whenRevisingTheItems_thenTheTotalFollowsTheNewList() {
        Shop shop = new Shop();
        String contactId = shop.contact("Cliente Revisão");
        String quoteId = shop.draft(contactId, shop.vehicle(contactId, plate()));

        Reply revised = shop.put("/api/quotes/" + quoteId + "/items", """
                {"items":[{"description":"Pastilha de freio","kind":"PART","quantity":"1","unitPrice":"150.00"}]}""");

        assertThat(revised.status()).as(revised.body()).isEqualTo(200);
        assertThat(revised.json().get("total").asString()).isEqualTo("150.00");
    }

    @Test
    void givenAConfirmedQuote_whenOpeningTheLink_thenTheClientSeesOnlyTheMinimumAndNothingIsConsumed() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Link", "KLM2N34");
        HttpApi anonymous = new HttpApi(port);

        Reply first = anonymous.get("/api/public/quotes/" + sent.token(), Map.of());
        Reply second = anonymous.get("/api/public/quotes/" + sent.token(), Map.of());

        assertThat(sent.url()).endsWith("/o/" + sent.token());
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body()).isEqualTo(second.body());
        assertThat(first.json().get("status").asString()).isEqualTo("SENT");
        assertThat(first.json().get("total").asString()).isEqualTo("131.00");
        assertThat(first.json().get("vehicleLabel").asString()).contains("Fiat", "Uno", "2014").endsWith("N34");
        assertThat(first.body()).doesNotContain("KLM2N34").doesNotContain("Cliente Link").doesNotContain("phone");
        assertThat(first.json().propertyNames()).containsExactlyInAnyOrder("shopName", "vehicleLabel", "items", "total",
                "status", "expiresAt");
    }

    @Test
    void givenThePublicQuoteRoutes_whenAnswering_thenNothingIsCachedIndexedOrReferred() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Cabeçalho", plate());
        HttpApi anonymous = new HttpApi(port);

        for (Reply reply : new Reply[] {anonymous.get("/api/public/quotes/" + sent.token(), Map.of()),
                anonymous.get("/api/public/quotes/" + "A".repeat(43), Map.of()),
                anonymous.post("/api/public/quotes/" + sent.token() + "/approve", "{}", Map.of()),
                anonymous.get("/o/" + sent.token(), Map.of())}) {
            assertThat(reply.header("Cache-Control")).contains("no-store");
            assertThat(reply.header("X-Robots-Tag")).contains("noindex").contains("nofollow");
            assertThat(reply.header("Referrer-Policy")).isEqualTo("no-referrer");
        }
    }

    @Test
    void givenTheLink_whenApprovingTwice_thenTheSecondCallChangesNothingAndTheOppositeIsRefused() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Decide", plate());
        HttpApi anonymous = new HttpApi(port);
        String approve = "/api/public/quotes/" + sent.token() + "/approve";

        Reply first = anonymous.post(approve, "{}", Map.of());
        Reply repeat = anonymous.post(approve, "{}", Map.of());
        Reply opposite = anonymous.post("/api/public/quotes/" + sent.token() + "/reject", "{\"reason\":\"mudei de ideia\"}", Map.of());

        assertThat(first.status()).isEqualTo(200);
        assertThat(first.json().get("status").asString()).isEqualTo("APPROVED");
        assertThat(repeat.status()).isEqualTo(200);
        assertThat(repeat.json().get("status").asString()).isEqualTo("APPROVED");
        assertThat(opposite.status()).isEqualTo(409);
        assertThat(opposite.json().get("code").asString()).isEqualTo("QUOTE_ALREADY_DECIDED");
        assertThat(shop.get("/api/quotes/" + sent.quoteId()).json().get("status").asString()).isEqualTo("APPROVED");
    }

    @Test
    void givenTheLink_whenRejectingWithAReason_thenTheOwnerSeesTheReason() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Recusa", plate());

        Reply rejected = new HttpApi(port).post("/api/public/quotes/" + sent.token() + "/reject",
                "{\"reason\":\"Achei caro\"}", Map.of());

        assertThat(rejected.status()).isEqualTo(200);
        assertThat(rejected.json().get("status").asString()).isEqualTo("REJECTED");
        Reply seen = shop.get("/api/quotes/" + sent.quoteId());
        assertThat(seen.json().get("decisionReason").asString()).isEqualTo("Achei caro");
        assertThat(shop.get("/api/quotes").json().get("closed").get(0).get("status").asString()).isEqualTo("REJECTED");
    }

    @Test
    void givenAnUnknownACancelledAndAMalformedLink_whenOpeningThem_thenTheAnswersAreIndistinguishable() {
        Shop shop = new Shop();
        Sent cancelled = shop.sentQuote("Cliente Cancelado", plate());
        assertThat(shop.post("/api/quotes/" + cancelled.quoteId() + "/cancel", "{}").status()).isEqualTo(200);
        HttpApi anonymous = new HttpApi(port);

        Reply unknown = anonymous.get("/api/public/quotes/" + "B".repeat(43), Map.of());
        Reply gone = anonymous.get("/api/public/quotes/" + cancelled.token(), Map.of());
        Reply malformed = anonymous.get("/api/public/quotes/curto", Map.of());
        Reply decisionOnGone = anonymous.post("/api/public/quotes/" + cancelled.token() + "/approve", "{}", Map.of());

        for (Reply reply : new Reply[] {unknown, gone, malformed, decisionOnGone}) {
            assertThat(reply.status()).isEqualTo(404);
            assertThat(reply.json().get("code").asString()).isEqualTo("LINK_UNAVAILABLE");
            assertThat(reply.json().get("title").asString()).isEqualTo(unknown.json().get("title").asString());
            assertThat(reply.body()).doesNotContain(cancelled.token());
        }
    }

    @Test
    void givenAResend_whenOwnerAsksAgain_thenTheLinkWorksAndTheStateDoesNotChange() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Reenvio", plate());

        Reply resent = shop.post("/api/quotes/" + sent.quoteId() + "/resend", "{}");

        assertThat(resent.status()).as(resent.body()).isEqualTo(200);
        String newToken = resent.json().get("publicUrl").asString();
        assertThat(new HttpApi(port).get("/api/public/quotes/" + newToken.substring(newToken.lastIndexOf('/') + 1), Map.of())
                .status()).isEqualTo(200);
        assertThat(shop.get("/api/quotes/" + sent.quoteId()).json().get("status").asString()).isEqualTo("SENT");
    }

    @Test
    void givenACancelledQuote_whenCancellingAgainOrEditing_thenCancelIsIdempotentAndEditIsRefused() {
        Shop shop = new Shop();
        Sent sent = shop.sentQuote("Cliente Cancela", plate());
        assertThat(shop.post("/api/quotes/" + sent.quoteId() + "/cancel", "{}").status()).isEqualTo(200);

        Reply cancelAgain = shop.post("/api/quotes/" + sent.quoteId() + "/cancel", "{}");
        Reply edit = shop.put("/api/quotes/" + sent.quoteId() + "/items", """
                {"items":[{"description":"X","kind":"LABOR","quantity":"1","unitPrice":"1.00"}]}""");

        assertThat(cancelAgain.status()).isEqualTo(200);
        assertThat(cancelAgain.json().get("status").asString()).isEqualTo("CANCELLED");
        assertThat(edit.status()).isEqualTo(409);
        assertThat(edit.json().get("code").asString()).isEqualTo("QUOTE_INVALID_STATE");
    }

    @Test
    void givenTwoShops_whenOneUsesTheIdsOfTheOther_thenEverythingLooksLikeItDoesNotExist() {
        Shop ana = new Shop();
        Shop beto = new Shop();
        Sent anas = ana.sentQuote("Cliente da Ana", plate());
        String anasContact = ana.contact("Outro da Ana");

        assertThat(beto.get("/api/quotes/" + anas.quoteId()).status()).isEqualTo(404);
        assertThat(beto.post("/api/quotes/" + anas.quoteId() + "/cancel", "{}").status()).isEqualTo(404);
        assertThat(beto.post("/api/quotes/" + anas.quoteId() + "/resend", "{}").status()).isEqualTo(404);
        assertThat(beto.put("/api/quotes/" + anas.quoteId() + "/items", """
                {"items":[{"description":"X","kind":"LABOR","quantity":"1","unitPrice":"1.00"}]}""").status()).isEqualTo(404);
        assertThat(beto.get("/api/contacts/" + anasContact + "/vehicles").status()).isEqualTo(404);
        assertThat(beto.post("/api/contacts/" + anasContact + "/vehicles", """
                {"plate":"%s","brand":"Fiat","model":"Uno","year":2014}""".formatted(plate())).status()).isEqualTo(404);
        assertThat(beto.get("/api/quotes").json().get("needsOwner").size()).isZero();
        assertThat(beto.get("/api/quotes").json().get("awaitingClient").size()).isZero();
        assertThat(ana.get("/api/quotes/" + anas.quoteId()).json().get("status").asString()).isEqualTo("SENT");
    }

    @Test
    void givenACompanyProfile_whenUpdatingIt_thenThePhoneComesBackMasked() {
        Shop shop = new Shop();

        Reply updated = shop.put("/api/company-profile", """
                {"displayName":"Oficina Fictícia","businessPhone":"(11) 93333-2222","address":"Rua Sintética, 10",
                 "openingHours":"Seg a sex, 8h às 18h","defaultValidityDays":5,"signatureMessage":"Obrigado!"}""");

        assertThat(updated.status()).as(updated.body()).isEqualTo(200);
        assertThat(updated.json().get("businessPhoneMasked").asString()).endsWith("-2222");
        assertThat(updated.body()).doesNotContain("93333");
        assertThat(shop.get("/api/company-profile").json().get("defaultValidityDays").asInt()).isEqualTo(5);
    }
}

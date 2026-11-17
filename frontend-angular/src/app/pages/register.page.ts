import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SessionStore } from '../session/session.store';
import { problemMessage } from '../session/problem-message';

const PASSWORD_MIN_LENGTH = 12;

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Cadastrar empresa</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <p>
        <label>Nome da empresa <input formControlName="companyName" autocomplete="organization" /></label>
      </p>
      <p>
        <label>Seu nome <input formControlName="ownerName" autocomplete="name" /></label>
      </p>
      <p>
        <label>E-mail <input type="email" formControlName="email" autocomplete="email" /></label>
      </p>
      <p>
        <label>Senha (mínimo 12 caracteres)
          <input type="password" formControlName="password" autocomplete="new-password" />
        </label>
      </p>
      <button type="submit" [disabled]="form.invalid || busy()">Cadastrar</button>
      @if (error()) {
        <p role="alert" data-testid="error">{{ error() }}</p>
      }
    </form>
    <p>Já tem conta? <a routerLink="/login">Entrar</a></p>
  `,
})
export class RegisterPage {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = new FormGroup({
    companyName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    ownerName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH), Validators.maxLength(128)],
    }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const { companyName, ownerName, email, password } = this.form.getRawValue();
    try {
      await this.session.register(companyName, ownerName, email, password);
      await this.session.login(email, password);
      await this.router.navigateByUrl('/fila');
    } catch (failure) {
      this.error.set(problemMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SessionStore } from '../session/session.store';
import { problemMessage } from '../session/problem-message';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Entrar</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <p>
        <label>E-mail <input type="email" formControlName="email" autocomplete="username" /></label>
      </p>
      <p>
        <label>Senha <input type="password" formControlName="password" autocomplete="current-password" /></label>
      </p>
      <button type="submit" [disabled]="form.invalid || busy()">Entrar</button>
      @if (error()) {
        <p role="alert" data-testid="error">{{ error() }}</p>
      }
    </form>
    <p>Ainda não tem conta? <a routerLink="/register">Cadastrar empresa</a></p>
  `,
})
export class LoginPage {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    try {
      await this.session.login(email, password);
      await this.router.navigateByUrl('/me');
    } catch (failure) {
      this.error.set(problemMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}

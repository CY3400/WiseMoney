import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class Common {
  constructor(private http: HttpClient, private router: Router){}

  redirection(location: string): void{
    this.router.navigate([location]);
  }

  private passwordVisibility = new Map<string, boolean>();

  setPasswordToggle(key: string, initialState: boolean = false): void {
    this.passwordVisibility.set(key, initialState);
  }

  togglePasswordVisibility(key: string): void {
    const current = this.passwordVisibility.get(key) ?? false;
    this.passwordVisibility.set(key, !current);
  }

  isPasswordVisible(key: string): boolean {
    return this.passwordVisibility.get(key) ?? false;
  }

  toggleAndGetVisibility(key: string): boolean {
    this.togglePasswordVisibility(key);
    return this.isPasswordVisible(key);
  }

  validateKey(event: KeyboardEvent, allowedKeys: string[], regex: RegExp): void {
    if(!allowedKeys.includes(event.key) && !regex.test(event.key)) {
      event.preventDefault();
    }
  }

  validatePaste(event: ClipboardEvent, regex: RegExp): void {
    event.preventDefault();
    const pasted = (event.clipboardData ?? (window as any).clipboardData)?.getData('text') ?? '';
    const sanitized = [...pasted].filter(c => regex.test(c)).join('');
    const input = event.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end = input.selectionEnd ?? 0;

    const newValue = input.value.slice(0, start) + sanitized + input.value.slice(end);
    input.value = newValue;

    const newCursor = start + sanitized.length;
    input.setSelectionRange(newCursor, newCursor);
  }

  validateNextValueKeydown(event: KeyboardEvent, valueRegex: RegExp, allowedKeys: string[] ) {
    const input = event.target as HTMLInputElement;
    const key = event.key;

    if (allowedKeys.includes(key) || key.length !== 1) return;

    if (!'0123456789.'.includes(key)) { event.preventDefault(); return; }

    const s = input.selectionStart ?? input.value.length;
    const e = input.selectionEnd   ?? input.value.length;
    const next = input.value.slice(0, s) + key + input.value.slice(e);

    if (!valueRegex.test(next)) event.preventDefault();
  }

  validateNextValuePaste(event: ClipboardEvent, valueRegex: RegExp): void {
    event.preventDefault();
    const input = event.target as HTMLInputElement;
    let text = (event.clipboardData ?? (window as any).clipboardData)?.getData('text') ?? '';
    text = text.replace(',', '.').replace(/[^\d.]/g, '');

    const s = input.selectionStart ?? input.value.length;
    const e = input.selectionEnd   ?? input.value.length;
    const candidate = input.value.slice(0, s) + text + input.value.slice(e);

    if (valueRegex.test(candidate)) {
      input.value = candidate;
      const caret = (input.value.slice(0, s) + text).length;
      input.setSelectionRange(caret, caret);
    }
  }

  noPaste(event: ClipboardEvent): void {
    event.preventDefault();
  }

  isValid(name: string, regex: RegExp, letter: RegExp): boolean {
    return regex.test(name.trim()) && letter.test(name.trim());
  }

  async isEmailUnique(email: string): Promise<boolean> {
    const response = await firstValueFrom(this.http.get<boolean>(`http://localhost:8080/verify?email=${email}`));
    return response;
  }

  hasErrors(errors: { [key: string]: string }): boolean {
    return Object.values(errors).some(e => e !== '');
  }
}
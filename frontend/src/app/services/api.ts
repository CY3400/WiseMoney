import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

  export type Id = number | string;
  export type CategoryTypeCode = 'DEPENSE' | 'REVENU';

  export interface CreateBudgetRequest {
    amount: number;
    month: number;
    year: number;
  }

  export interface Budget {
    id?: number | string;
    amount: number;
    month: number;
    year: number;
    [k: string]: any;
  }

  export interface CategoryDto {
    id: Id;
    name: string;
    type: string;
    parentId?: Id | null;
    status: number;
  }

  export interface ParentCategoryDto {
    id: number | string;
    name: string;
    displayName?: string;
    type: CategoryTypeCode;
  }

  export interface ChildCategoryDto {
    id: number | string;
    name: string;
    displayName?: string;
  }

  export interface CreateCategoryRequest {
    name: string;
    type: string;
    parentId?: Id | null;
  }

  export interface CategoryRaw {
    id: Id;
    name: string;
    type?: string;
    status?: number;
    parentId?: Id | null;
    parent?: Id | {id: Id; name?: string} | null;
    parent_id?: Id | null;
    parentID?: Id | null;
  }

  export interface TransactionRequest {
    category: Id;
    amount: string
    transactionDate: string
    notes: string;
  }

  export interface TransactionDto {
    id: Id;
    category: Id;
    amount: string;
    date: string;
    notes: string;
  }

  export interface GestionBudgetRequest {
    category: Id;
    amount: string;
    type: string;
  }

  export interface GestionBudgetDto {
    id: Id;
    category: Id;
    amount: string;
    type: string;
  }

@Injectable({
  providedIn: 'root'
})
export class Api {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient){}

  private normalizeCategory = (c: CategoryRaw): CategoryDto => {
    let parentId: Id | null = c.parentId ?? (typeof c.parent === 'object' && c.parent !== null ? (c.parent as any).id ?? null : typeof c.parent === 'number' || typeof c.parent === 'string' ? (c.parent as any) : null) ?? c.parent_id ?? c.parentID ?? null;

    return {
      id: c.id,
      name: c.name,
      type: c.type ?? '',
      status: c.status ?? 1,
      parentId
    };
  }

  register(user: any): Observable<any>{
    return this.http.post(`${this.baseUrl}/register`, user, {
      withCredentials: true
    });
  }

  login(user: any): Observable<any>{
    return this.http.post(`${this.baseUrl}/login`, user, {
      withCredentials: true
    });
  }

  logout(): Observable<any>{
    return this.http.post(`${this.baseUrl}/logout`, {}, {
      withCredentials: true
    });
  }

  me(): Observable<any> {
    return this.http.get(`${this.baseUrl}/me`, {
      withCredentials: true
    });
  }

  changePassword(payload: any): Observable<{message: string}> {
    return this.http.post<{message: string}>(`${this.baseUrl}/change-password`, payload, {
      withCredentials: true
    });
  }

  updateMe(payload: {firstName: string; lastName: string}): Observable<any>{
    return this.http.put(`${this.baseUrl}/me`, payload, {
      withCredentials: true
    });
  }

  forgotPassword(email: string) {
    return this.http.post<{ message: string; resetUrl?: string }>(
      `${this.baseUrl}/forgot-password`,
      { email },
      { withCredentials: true }
    );
  }

  resetPassword(payload: {token: string; newPassword: string}){
    return this.http.post<{message: string}>(`${this.baseUrl}/reset-password`, payload, {
      withCredentials: true
    });
  }

  createBudget(payload: CreateBudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(`${this.baseUrl}/budgets`, payload, {
      withCredentials: true
    })
  }

  getBudget(): Observable<Budget[]> {
    const now = new Date();
    const params = new HttpParams()
      .set('year', now.getFullYear())
      .set('month', now.getMonth() + 1)
    return this.http.get<Budget[]>(`${this.baseUrl}/budgets`, {
      params,
      withCredentials: true,
    });
  }

  updateBudget(id: number | string, payload: { amount: number }): Observable<Budget> {
    return this.http.put<Budget>(`${this.baseUrl}/budgets/${id}`, payload, {
      withCredentials: true
    })
  }

  createCategory(payload: CreateCategoryRequest): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${this.baseUrl}/categories`, payload, {
      withCredentials: true
    })
  }

  getCategories(): Observable<CategoryDto[]> {
    return this.http
      .get<CategoryRaw[]>(`${this.baseUrl}/categories`, { withCredentials: true })
      .pipe(map(list => (list ?? []).map(this.normalizeCategory)));
  }

  updateCategory(id: Id, payload: {name?: string; parentId?: Id | null}): Observable<CategoryDto>{
    const body = {name: payload.name, parentId: payload.parentId ?? null};
    return this.http.put<CategoryRaw>(`${this.baseUrl}/categories/${id}`, body, {
      withCredentials: true
    }).pipe(map(this.normalizeCategory));
  }

  toggleCategoryStatus(id: Id): Observable<CategoryDto> {
    return this.http.put<CategoryRaw>(`${this.baseUrl}/categories/${id}/toggle`, {}, {
      withCredentials: true
    }).pipe(map(this.normalizeCategory));
  }

  getCategoryTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/category-types`, {
      withCredentials: true
    })
  }

  getCategoryParents(type?: string, onlyActive: boolean = true) {
    const params: any = {};
    if(type) params.type = type;
    if(onlyActive !== undefined) params.onlyActive = String(onlyActive);
    return this.http.get<ParentCategoryDto[]>(`${this.baseUrl}/categories/parents`, {
      params,
      withCredentials: true
    })
  }

  getCategoryChildren() {
    return this.http.get<ChildCategoryDto[]>(`${this.baseUrl}/categories/children`, {
      withCredentials: true
    })
  }

  createTransaction(payload: TransactionRequest): Observable<TransactionDto> {
    return this.http.post<TransactionDto>(`${this.baseUrl}/transactions`, payload, {
      withCredentials: true
    })
  }

  updateTransaction(id: Id, payload: TransactionRequest): Observable<TransactionDto>{
    return this.http.put<TransactionDto>(`${this.baseUrl}/transactions/${id}`, payload, {
      withCredentials: true
    });
  }

  getTransactions(): Observable<TransactionDto[]> {
    return this.http
      .get<TransactionDto[]>(`${this.baseUrl}/transactions`, { withCredentials: true });
  }

  deleteTransaction(id: Id): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transactions/${id}`, { withCredentials: true });
  }

  createBudgetManagement(payload: GestionBudgetRequest): Observable<GestionBudgetDto>{
    return this.http.post<GestionBudgetDto>(`${this.baseUrl}/management`, payload, {
      withCredentials: true
    })
  }

  updateBudgetManagement(id: Id, payload: GestionBudgetRequest): Observable<GestionBudgetDto>{
    return this.http.put<GestionBudgetDto>(`${this.baseUrl}/management/${id}`, payload, {
      withCredentials: true
    });
  }

  getBudgetManagement(): Observable<GestionBudgetDto[]> {
    return this.http.get<GestionBudgetDto[]>(`${this.baseUrl}/management`, { withCredentials: true });
  }
}
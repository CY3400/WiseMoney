import { Routes } from '@angular/router';
import { Register } from './pages/register/register';
import { Welcome } from './pages/welcome/welcome';
import { Login } from './pages/login/login';
import { Home } from './pages/home/home';
import { Myaccount } from './pages/myaccount/myaccount';
import { Changepassword } from './pages/changepassword/changepassword';
import { MeResolver } from './resolvers/me-resolver';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ResetPassword } from './pages/reset-password/reset-password';
import { Budget } from './pages/budget/budget';
import { Category } from './pages/category/category';
import { Transaction } from './pages/transaction/transaction';
import { GestionBudget } from './pages/gestion-budget/gestion-budget';
import { Statistics } from './pages/statistics/statistics';
import { Objectifs } from './pages/objectifs/objectifs';

export const routes: Routes = [
    {path: '', component: Welcome, data: {public: true}},
    {path:'bienvenue', component: Welcome, data: {public: true}},
    {path:'s-enregistrer', component: Register, title: 'WiseMoney - Inscription', data: {public: true}},
    {path:'se-connecter', component: Login, title: 'WiseMoney - Connexion', data: {public: true}},
    {path:'accueil', component: Home, title: 'WiseMoney - Accueil', resolve: {me: MeResolver}},
    {path:'mon-compte', component: Myaccount, title: 'WiseMoney - Mon Compte', resolve: {me: MeResolver}},
    {path:'changer-mdp', component: Changepassword, title: 'WiseMoney - Changer le mot de passe', resolve: {me: MeResolver}},
    { path:'mot-de-passe-oublie', component: ForgotPassword, title:'WiseMoney - Mot de passe oublié' , data: {public: true}},
    { path:'reset-mdp', component: ResetPassword, title:'WiseMoney - Réinitialiser le mot de passe' , data: {public: true}},
    { path:'budget', component: Budget, title:'WiseMoney - Budget' , resolve: {me: MeResolver}},
    { path:'categorie', component: Category, title:'WiseMoney - Category', resolve: {me: MeResolver}},
    { path:'transaction', component: Transaction, title:'WiseMoney - Transaction', resolve: {me: MeResolver}},
    { path:'gestion-budget', component: GestionBudget, title:'WiseMoney - Gestion du Budget', resolve: {me: MeResolver}},
    { path:'statistiques', component: Statistics, title:'WiseMoney - Statistiques', resolve: {me: MeResolver}},
    { path:'objectif', component: Objectifs, title:'WiseMoney - Objectifs', resolve: {me: MeResolver}}
];
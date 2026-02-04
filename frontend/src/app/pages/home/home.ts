import { Component, OnInit, TrackByFunction } from '@angular/core';
import { Api, PSV, CPV } from '../../services/api';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { InsightDTO } from '../../services/api';
import type { Budget as BudgetDto } from '../../services/api';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css','../../../styles.css']
})
export class Home implements OnInit {
  user: any = null;
  percent: number = 0;
  progressColor: string = '#bfbfbf';
  conicBackground = 'conic-gradient(#bfbfbf 0%, #e6e6e6 0%)';
  psv: PSV[] = [];
  loadingPSV = false;
  epargne = 0;
  average = 0;
  budget = 0;
  total = 0;
  sumRev = 0;
  sumDep = 0;
  sumDepF = 0;
  currentBudgetId: number | string | null = null;
  expandedNames = new Set<string>();
  childrenMap = new Map<number, CPV[]>();
  loadingChildren = new Set<number>();
  trackByParent: TrackByFunction<PSV> = (_: number, item: PSV) => (item?.parentId as number) ?? (item?.name as string);
  trackByChild: TrackByFunction<CPV> = (_: number, item: CPV) => (item?.childId as number) ?? (item?.name as string);
  insights: InsightDTO[] = [];
  loadingInsights = false;

  insightMonth = '';
  monthInsight = '';

  constructor(private api: Api, private router: Router, private route: ActivatedRoute) {
    this.user = this.route.snapshot.data['me'];
  }

  ngOnInit(): void {
    this.insightMonth = this.getYearMonth(new Date());

    this.monthInsight = new Date().toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' }).replace(/^./, c => c.toUpperCase());

    this.loadPercent();
    this.loadPSV();
    this.loadEpargne();
    this.loadAverage();
    this.loadBudget();
    this.loadSumRev();
    this.loadSumDep();
    this.loadSumDepF();

    this.loadInsights(true);
  }

    private getYearMonth(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  }

  loadBudget(): void{
    this.api.getBudget().subscribe({
      next: (list: BudgetDto[]) => {
        this.total = (list ?? []).reduce((sum, b) => {
          const raw = b?.amount as unknown;
          const n =
            typeof raw === 'number'
              ? raw
              : parseFloat(String(raw).replace(',', '.'));
          return sum + (Number.isFinite(n) ? n : 0);
        }, 0);

        const chosen = (list ?? [])[0];
        this.currentBudgetId = chosen?.id ?? null;

        this.budget = this.total > 0 ? this.total : 0;
      },
      error: () => {
        this.total = 0;
        this.currentBudgetId = null;
        this.budget = 0;
      }
    });
  }

  loadSumRev(): void {
    this.api.getSumRev().subscribe({
      next: (ep) => {
        this.sumRev = ep ?? 0;
      },
      error: (err) => {
        console.error('Erreur getSumRev():', err);
        this.sumRev = 0;
      }
    })
  }

  loadSumDep(): void {
    this.api.getSumDep().subscribe({
      next: (ep) => {
        this.sumDep = ep ?? 0;
      },
      error: (err) => {
        console.error('Erreur getSumDep():', err);
        this.sumDep = 0;
      }
    })
  }

  loadSumDepF(): void {
    this.api.getSumDepF().subscribe({
      next: (ep) => {
        this.sumDepF = ep ?? 0;
      },
      error: (err) => {
        console.error('Erreur getSumDepF():', err);
        this.sumDepF = 0;
      }
    })
  }

  loadInsights(force: boolean): void {
    this.loadingInsights = true;

    this.api.getInsights(this.insightMonth, force).subscribe({
      next: (rows) => {
        this.insights = rows ?? [];
        this.loadingInsights = false;
      },
      error: (err) => {
        console.error('Erreur getInsights():', err);
        this.insights = [];
        this.loadingInsights = false;
      }
    });
  }

  severityLabel(s: string): string {
    if (s === 'CRITICAL') return 'Critique';
    if (s === 'WARNING') return 'Attention';
    return 'Info';
  }

  severityColor(s: string): string {
    if (s === 'CRITICAL') return '#e74c3c';
    if (s === 'WARNING') return '#f1c40f';
    return '#2ecc71';
  }

  loadPercent(): void {
    this.api.getCurrentPercentMonth().subscribe({
      next: (res:number) => {
        this.percent = res ?? 0;
        this.updateProgressColor();
        this.updateConicBackground();
      },
      error: (err) => {
        console.error('Erreur lors du chargement du pourcentage :', err);
        this.percent = 0;
        this.updateProgressColor();
        this.updateConicBackground();
      }
    });
  }

  updateProgressColor(): void {
    if (this.percent === 0) {
      this.progressColor = '#bfbfbf';
    }
    else if (this.percent <= 25) {
      this.progressColor = '#2ecc71';
    }
    else if (this.percent <= 50) {
      this.progressColor = '#f1c40f';
    }
    else if (this.percent <= 75) {
      this.progressColor = '#e67e22';
    }
    else {
      this.progressColor = '#e74c3c';
    }
  }

  updateConicBackground(): void {
    this.conicBackground = `conic-gradient(${this.progressColor} ${this.percent}%, #e6e6e6 0)`;
  }

  loadPSV(): void {
    this.loadingPSV = true;
    this.api.getPSV().subscribe({
      next: (rows) => {
        this.psv = (rows ?? [])
          .map(r => ({
            parentId: Number((r as any).parentId ?? (r as any).parent_id ?? 0),
            name: String(r.name),
            total: r.total
          }))
          .sort((a, b) => b.total - a.total);

        this.loadingPSV = false;
      },
      error: (err) => {
        console.error('Erreur getPSV():', err);
        this.psv = [];
        this.loadingPSV = false;
      }
    });
  }

  loadEpargne(): void {
    this.api.getEpargne().subscribe({
      next: (ep) => {
        this.epargne = ep ?? 0;
      },
      error: (err) => {
        console.error('Erreur getEpargne():', err);
        this.epargne = 0;
      }
    })
  }

  loadAverage(): void {
    this.api.getAverage().subscribe({
      next: (ep) => {
        this.average = ep ?? 0;
      },
      error: (err) => {
        console.error('Erreur getAverage():', err);
        this.average = 0;
      }
    })
  }

  barColor(p: number): string {
    if (p === 0) return '#bfbfbf';
    if (p <= 25) return '#2ecc71';
    if (p <= 50) return '#f1c40f';
    if (p <= 75) return '#e67e22';
    return '#e74c3c';
  }

  onToggleParent(p: PSV): void {
    const wasOpen = this.isExpanded(p.name);
    this.toggleExpand(p.name);

    if(!wasOpen) {
      if (!p.parentId) {
        console.warn('parentId manquant pour ', p);
        return;
      }
      if (!this.childrenMap.has(p.parentId) && !this.loadingChildren.has(p.parentId)) {
        this.loadChildrenForParent(p.parentId);
      }
    }
  }

  private loadChildrenForParent(parentId: number): void {
    this.loadingChildren.add(parentId);
    this.api.getCPV(parentId).subscribe({
      next: (rows) => {
        const list: CPV[] = (rows ?? []).map(r => ({
          childId: Number((r as any).childId ?? (r as any).child_id ?? 0),
          name: String(r.name),
          amount: Number(r.amount ?? 0),
          percent: Number(r.percent)
        }));
        this.childrenMap.set(parentId, list);
        this.loadingChildren.delete(parentId);
      },
      error: (err) => {
        console.error(`Erreur getChildrenPercents(${parentId}):`, err);
        this.childrenMap.set(parentId, []);
        this.loadingChildren.delete(parentId);
      }
    });
  }

  toggleExpand(name: string): void {
    if(this.expandedNames.has(name)){
      this.expandedNames.delete(name);
    }
    else {
      this.expandedNames.add(name);
    }
  }

  isExpanded(name: string): boolean {
    return this.expandedNames.has(name);
  }

  logout(): void {
    this.api.logout().subscribe({
      next: () => {
        this.router.navigate(['/bienvenue']);
      }
    })
  }
}
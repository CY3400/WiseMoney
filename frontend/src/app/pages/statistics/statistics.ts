import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { Chart } from 'chart.js/auto';
import { ChartConfiguration } from 'chart.js';
import { Api, Gap, SMStats, Top } from '../../services/api';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [],
  templateUrl: './statistics.html',
  styleUrls: ['./statistics.css']
})
export class Statistics implements AfterViewInit, OnDestroy {
  @ViewChild('barChart') barChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieChart') pieChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('lineChart', {static: false}) lineChart!: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;
  private pie?: Chart;
  private line?: Chart;
  currentAmount: string = '';
  gap: number = 0;
  isPositive: boolean = true;

  constructor(private api: Api) {}

  ngAfterViewInit(): void {
    this.loadData();
    this.loadTopExpenses();
    this.loadGap();
    this.loadDiff();
  }

  ngOnDestroy(): void {
    if(this.chart) {
      this.chart.destroy();
    }

    if (this.pie) {
      this.pie.destroy();
    }

    if (this.line) {
      this.line.destroy();
    }
  }

  private loadData(): void {
    this.api.getSixMonthsStats().subscribe({
      next: (data: SMStats[]) => {
        const stats = [...data].reverse();
        this.buildChart(stats);
      },
      error: (err) => {
        console.error('Erreur chargement stats', err);
      }
    });
  }

  private loadTopExpenses(): void {
    this.api.getTopByMonth().subscribe({
      next: (data: Top[]) => {
        this.buildPie(data);
      },
      error: (err) => {
        console.error('Erreur chargement top 5 dépenses', err);
      }
    });
  }

  private loadGap(): void {
    this.api.getGapOfMonth().subscribe({
      next: (data: Gap[]) => {
        if(!data || data.length === 0)
          return;

        const info = data[0];

        this.currentAmount = info.currentAmount;
        this.gap = info.gap;
        this.isPositive = info.gap > 0;
      },
      error: (err) => {
        console.error('Erreur chargement écart', err);
      }
    });
  }

  private loadDiff(): void {
    this.api.getExpensesRevenuesDifference().subscribe({
      next: (data: SMStats[]) => {
        this.buildLine(data);
      },
      error: (err) => {
        console.error('Erreur chargement diifférence dépenses revenus', err);
      }
    })
  }

  private buildChart(stats: SMStats[]): void {
    if(this.chart) {
      this.chart.destroy();
    }

    const labels = stats.map(s => this.formatLabel(s.month, s.year));
    const revenus = stats.map(s => Number(s.revenues));
    const depenses = stats.map(s => Number(s.expenses));
    const epargnes = stats.map(s => Number(s.savings));

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Revenu',
            data: revenus,
            backgroundColor: 'rgba(75, 192, 192, 0.7)'
          },
          {
            label: 'Dépense',
            data: depenses,
            backgroundColor: 'rgba(255, 99, 132, 0.7)'
          },
          {
            label: 'Épargne',
            data: epargnes,
            backgroundColor: 'rgba(54, 162, 235, 0.7)'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            stacked: false
          },
          y: {
            beginAtZero: true
          }
        },
        plugins: {
          legend: {
            position: 'top'
          },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const value = ctx.parsed.y ?? 0;
                return `${ctx.dataset.label}: ${value.toLocaleString('fr-FR', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0
                })}`;
              }
            }
          }
        }
      }
    };

    const ctx = this.barChart.nativeElement.getContext('2d');
    if(!ctx) return;

    this.chart = new Chart(ctx, config);
  }

  private buildPie(items: Top[]): void {
    if(this.pie) {
      this.pie.destroy();
    }

    const labels = items.map(i => i.name);
    const values = items.map(i => Number(i.total));

    const config: ChartConfiguration<'pie'> = {
      type: 'pie',
      data: {
        labels,
        datasets: [
          {
            label: 'Top 5 des dépenses',
            data: values,
          }
        ]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'right'
          },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const label = ctx.label || '';
                const value = ctx.parsed || 0;

                return `${label}: ${value.toLocaleString('fr-FR', {
                  maximumFractionDigits: 0
                })}`;
              }
            }
          }
        }
      }
    };

    const ctx = this.pieChart.nativeElement.getContext('2d');
    if (!ctx) return;

    this.pie = new Chart(ctx, config);
  }

  private buildLine(stats: SMStats[]): void {
    if(this.line) {
      this.line.destroy();
    }

    const labels = stats.map(s => this.formatLabel(s.month, s.year));
    const diffData = stats.map(s => Number(s.revenues) - Number(s.expenses));

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Revenus - Dépenses',
            data: diffData,
            fill: false,
            tension: 0.3,
            borderWidth: 3,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: false
          }
        },
        plugins: {
          legend: {
            position: 'top'
          },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const value = ctx.parsed.y ?? 0;
                return `${value.toLocaleString('fr-FR', {
                  minimumFractionDigits: 0,
                  maximumFractionDigits: 0,
                })} LBP`;
              }
            }
          }
        }
      }
    };

    const ctx = this.lineChart.nativeElement.getContext('2d');
    if(!ctx) return;

    this.line = new Chart(ctx, config);
  }

  private formatLabel(month: number, year: number): string {
    const m = month.toString().padStart(2, '0');
    return `${m}/${year}`;
  }
}

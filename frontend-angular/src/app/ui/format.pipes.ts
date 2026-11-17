import { Pipe, PipeTransform } from '@angular/core';
import { formatBRL, formatDayLong, formatDayShort } from '../cash/format';

@Pipe({ name: 'moeda' })
export class MoedaPipe implements PipeTransform {
  transform(cents: number, explicitSign = false): string {
    return formatBRL(cents, { explicitSign });
  }
}

@Pipe({ name: 'dataCurta' })
export class DataCurtaPipe implements PipeTransform {
  transform(date: string): string {
    return formatDayShort(date);
  }
}

@Pipe({ name: 'dataLonga' })
export class DataLongaPipe implements PipeTransform {
  transform(date: string): string {
    const label = formatDayLong(date);
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}

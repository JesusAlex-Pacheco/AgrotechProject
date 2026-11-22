// Pruebas de las utilidades de formato y roles (npm test).
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { cantidad, dia, hoyISO, pesos, promedioCalificaciones } from './formato.js';
import { esAdmin, esAgricultor, esCliente, gestionaVentas, puedeComprar } from './roles.js';

// Intl usa espacios especiales (no separables) en algunos formatos: se normalizan
const texto = (valor) => valor.replace(/\s/g, ' ');

describe('formato', () => {
  it('pesos colombianos sin decimales', () => {
    assert.equal(texto(pesos(4200)), '$ 4.200');
    assert.equal(texto(pesos('125000.50')), '$ 125.001');
    assert.equal(texto(pesos(null)), '$ 0');
  });

  it('cantidades con hasta dos decimales', () => {
    assert.equal(cantidad(2.5), '2,5');
    assert.equal(cantidad('1.256'), '1,26');
  });

  it('una fecha sin hora del backend no se corre al dia anterior', () => {
    // new Date('2026-12-01') seria medianoche UTC: en Colombia (UTC-5) daria 30 de noviembre
    // Segun la version de los datos de idioma sale "1/12/2026" o "1 dic 2026": ambos son el 1 de diciembre
    assert.match(dia('2026-12-01'), /^1\D+(dic|12)\D+2026$/);
    assert.equal(dia(null), '—');
  });

  it('hoyISO tiene el formato AAAA-MM-DD', () => {
    assert.match(hoyISO(), /^\d{4}-\d{2}-\d{2}$/);
  });

  it('promedio de calificaciones con un decimal', () => {
    assert.equal(promedioCalificaciones([{ calificacion: 5 }, { calificacion: 4 }, { calificacion: 4 }]), 4.3);
    assert.equal(promedioCalificaciones([{ calificacion: 3 }]), 3);
    assert.equal(promedioCalificaciones([]), null);
    assert.equal(promedioCalificaciones(undefined), null);
  });
});

describe('roles', () => {
  const cliente = { rol: 'CLIENTE', idPerfil: 5 };
  const agricultor = { rol: 'AGRICULTOR', idPerfil: 7 };
  const admin = { rol: 'ADMIN', idPerfil: null };
  const transportador = { rol: 'TRANSPORTADOR', idPerfil: 2 };

  it('sin sesion o como cliente se puede armar el carrito', () => {
    assert.equal(puedeComprar(null), true);
    assert.equal(puedeComprar(cliente), true);
    assert.equal(puedeComprar(agricultor), false);
    assert.equal(puedeComprar(admin), false);
  });

  it('cliente y agricultor necesitan su perfil', () => {
    assert.equal(esCliente(cliente), true);
    assert.equal(esCliente({ rol: 'CLIENTE', idPerfil: null }), false);
    assert.equal(esAgricultor(agricultor), true);
    assert.equal(esAgricultor(cliente), false);
  });

  it('el administrador no tiene perfil y lo puede todo', () => {
    assert.equal(esAdmin(admin), true);
    assert.equal(esAdmin(cliente), false);
    assert.equal(esAdmin(null), false);
  });

  it('gestionan ventas el agricultor y el administrador', () => {
    assert.equal(gestionaVentas(agricultor), true);
    assert.equal(gestionaVentas(admin), true);
    assert.equal(gestionaVentas(cliente), false);
    assert.equal(gestionaVentas(transportador), false);
  });
});

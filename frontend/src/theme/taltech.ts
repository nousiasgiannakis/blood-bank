import { registerTheme } from '@helex/ui';

/**
 * The TalTech theme — token-for-token the `taltech` pack from
 * helex-solutions/helex-extensions (packs/taltech.js), built from the
 * published TalTech design system (CVI 2022).
 *
 * The pack itself targets the EMR shell (it registers through
 * window.__helexPlatform before first render); this standalone app has no
 * shell, so we register the same definition directly with @helex/ui. The
 * one constraint carries over: registration must run at module top level,
 * BEFORE the first render — @helex/state resolves the active theme when it
 * is evaluated, and a theme registered later is silently discarded. That is
 * why main.tsx imports this module first.
 */
registerTheme('taltech', {
  name: 'TalTech',
  colorPrimary: '#342b60',
  colorPrimaryHover: '#272048',
  colorPrimaryLight: '#ebeaef',
  colorSecondary: '#e4067e',
  colorSecondaryHover: '#ab055f',
  colorAccent: '#4dbed2',
  accentBar: ['#e4067e', '#aa1352', '#342b60'],
  colorSidebar: '#342b60',
  colorSidebarText: '#cccad7',
  colorSidebarActive: '#e4067e',
  colorSuccess: '#277257',
  colorWarning: '#80642d',
  colorError: '#af4458',
  colorInfo: '#2468b0',
  colorTextPrimary: '#25262c',
  colorTextSecondary: '#4a4b58',
  colorTextDisabled: '#9396b0',
  colorBgPage: '#f6f6f8',
  colorBgCard: '#ffffff',
  colorBgHover: '#ededf2',
  colorBorder: '#ededf2',
  controlBorderColor: '#6e7184',
  fontFamily: "'Proxima Nova', Verdana, system-ui, sans-serif",
  borderRadiusSm: 2,
  borderRadiusMd: 4,
  borderRadiusLg: 8,
});

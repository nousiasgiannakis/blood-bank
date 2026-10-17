import { StrictMode, type ReactNode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { AuthProvider, MockAuthProvider } from '@helex/core';
import {
  createHelexStore,
  HelexI18nBridge,
  HelexQueryProvider,
  AuthReduxBridge,
  GlobalApiErrorBridge,
} from '@helex/state';
import { AppRoot, HelexThemeProvider } from '@helex/ui';
import { App } from './App';
/* Side-effect import: registers the TalTech theme BEFORE first render — see
   the note inside the module for why the ordering matters. */
import './theme/taltech';
/* Proxima Nova webfonts — the first name in the theme's fontFamily stack. */
import './assets/fonts/fonts.css';
/* @helex/ui's own stylesheet. The published package does not list it in its
   `exports` map, so `import '@helex/ui/dist/index.css'` is refused — importing the
   file by path is not. It carries the layout of ResourceForm (main column,
   sidebar, resize handle) and of the calendar family; without it those render
   unstyled. Keep this line. */
import '../node_modules/@helex/ui/dist/index.css';

/** Theme comes from the environment (frontend/.env), TalTech by default. */
const THEME = import.meta.env.VITE_THEME || 'taltech';

// The standard Helex provider stack, exactly as the production applications
// nest it (compare helex-tx modules/tedy/frontend/src/main.tsx). You should
// not need to change this file — your pages go under src/pages.
const store = createHelexStore();

/**
 * Dev auth: auto-sign-in a mock user, no login screen, no Keycloak. The
 * MockAuthProvider sends `Authorization: Bearer <username>` on every request —
 * the backend's MockBearerAuthFilter (auth.mock.enabled=true) accepts it.
 * Production builds use the real AuthProvider instead.
 */
const AuthGate = ({ children }: { children: ReactNode }) =>
  import.meta.env.DEV ? (
    <MockAuthProvider autoSignIn defaultUser="superadmin">
      {children}
    </MockAuthProvider>
  ) : (
    <AuthProvider>{children}</AuthProvider>
  );

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <HelexI18nBridge>
        <HelexQueryProvider>
          <AuthGate>
            <AuthReduxBridge />
            <BrowserRouter>
              <HelexThemeProvider theme={THEME}>
                <AppRoot appName="Animals Register">
                  <GlobalApiErrorBridge />
                  <App />
                </AppRoot>
              </HelexThemeProvider>
            </BrowserRouter>
          </AuthGate>
        </HelexQueryProvider>
      </HelexI18nBridge>
    </Provider>
  </StrictMode>,
);

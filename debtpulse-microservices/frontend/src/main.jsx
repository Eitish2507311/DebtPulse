import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './styles/theme.css';
import { Provider } from 'react-redux';
import App from './App.jsx';
import { store } from './store/index.js';
import { AuthProvider } from './auth/AuthContext.jsx';
import { ToastProvider } from './components/ToastHost.jsx';
import { PreferencesProvider } from './components/Preferences.jsx';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <PreferencesProvider>
          <ToastProvider>
            <AuthProvider>
              <App />
            </AuthProvider>
          </ToastProvider>
        </PreferencesProvider>
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
);

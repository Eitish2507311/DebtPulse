import { configureStore } from '@reduxjs/toolkit';
import notifications from './notificationsSlice.js';

/** Application Redux store. Currently holds the notification badge state (see notificationsSlice). */
export const store = configureStore({
  reducer: { notifications },
});

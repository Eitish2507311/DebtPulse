import { configureStore } from '@reduxjs/toolkit';
import { useDispatch, useSelector, type TypedUseSelectorHook } from 'react-redux';
import notifications from './notificationsSlice';

/** Application Redux store. Currently holds the notification badge state (see notificationsSlice). */
export const store = configureStore({
  reducer: { notifications },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

/** Typed hooks — use these instead of the plain useDispatch/useSelector. */
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

/**
 * Redux Toolkit slice for the global notification badge. This is the one place we use Redux
 * (the rest of the app uses React Context) — a deliberate, self-contained showcase of the
 * store / slice / reducer / selector pattern. The unread count is written by the Layout's
 * polling effect and read by the topbar bell.
 */
interface NotificationsState {
  unread: number;
}

const initialState: NotificationsState = { unread: 0 };

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setUnread: (state, action: PayloadAction<number>) => { state.unread = action.payload; },
    clearUnread: (state) => { state.unread = 0; },
  },
});

export const { setUnread, clearUnread } = notificationsSlice.actions;
export default notificationsSlice.reducer;

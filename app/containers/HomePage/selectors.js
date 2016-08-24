
/**
 * Homepage selectors
 */

import { createSelector } from 'reselect';

const selectHome = () => (state) => state.get('home');

const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get('layers').toJS()
);

const selectLoading = () => createSelector(
  selectHome(),
  (homeState) => homeState.get('loading')
);

export {
  selectLayers,
  selectLoading
}

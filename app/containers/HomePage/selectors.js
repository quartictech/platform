
/**
 * Homepage selectors
 */

import { createSelector } from 'reselect';

const selectHome = () => (state) => state.get('home');

const selectLayers = () => createSelector(
  selectHome(),
  (homeState) => homeState.get('layers')
);

export {
  selectLayers,
}

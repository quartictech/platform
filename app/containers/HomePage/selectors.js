
/**
 * Homepage selectors
 */

import { createSelector } from 'reselect';

const selectLayers = () => (state) => state.get('layers');

export {
  selectLayers,
}

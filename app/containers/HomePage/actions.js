import { IMPORT_LAYER, IMPORT_LAYER_DONE } from './constants';

export function importLayer() {
  return {
    type: IMPORT_LAYER,
  };
}

export function importLayerDone(layerId) {
  return {
    type: IMPORT_LAYER_DONE,
    layerId,
  };
}

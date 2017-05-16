export const UI_SET_ACTIVE_MODAL = "UI_SET_ACTIVE_MODAL";
export const CREATE_NOTE = "CREATE_NOTE";


///////////////////////////////////////////////////
// API stuff
///////////////////////////////////////////////////

export interface ApiConstants {
  required: string,
  beganLoading: string,
  loaded: string,
  failedToLoad: string,
};

const createApiConstants = (prefix: String) => <ApiConstants> {
  required: `API/${prefix}/REQUIRED`,
  beganLoading: `API/${prefix}/BEGAN_LOADING`,
  loaded: `API/${prefix}/LOADED`,
  failedToLoad: `API/${prefix}/REQUIRED`,
};

export const ASSETS = createApiConstants("ASSETS");
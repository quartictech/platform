import { reducer } from "../../api-management";
import { assets } from "../../api";

// TODO: we're no longer handling CREATE_NOTE
export const assetsReducer = reducer(assets);


/**
 * Object Assign Helper
 */
export function assign(target: any, ...sources): any {
  return Object.assign({}, target, ...sources);
}

export const QUARTIC_XSRF = "quartic-xsrf";
export const QUARTIC_XSRF_HEADER = "X-XSRF-Token";

/**
 * Object Assign Helper
 */
export function assign(target: any, ...sources): any {
  return Object.assign({}, target, ...sources);
}

export const stringInString = (needle: string, haystack: string) =>
  (haystack.toLocaleLowerCase().indexOf(needle.toLocaleLowerCase()) !== -1);

// From http://stackoverflow.com/a/196991
export const toTitleCase = (str) =>
  str.replace(/\w\S*/g, txt => txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase());


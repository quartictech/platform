
declare module "react-sizeme" {
  function SizeMe<T>(): (c: React.ComponentClass<T>) => React.ComponentClass<T>;  // tslint:disable-line:function-name
  export default SizeMe;
}

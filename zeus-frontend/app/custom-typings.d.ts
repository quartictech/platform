
declare module "react-sizeme" {
  function SizeMe<T>(): (c: React.ComponentClass<T>) => React.ComponentClass<T>;
  export default SizeMe;
}

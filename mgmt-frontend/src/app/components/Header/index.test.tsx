import * as React from "react";
import { mount } from "enzyme";
import { expect } from "chai";
import { Header } from "./";

/** Data */
const s = require("./style.css");

/** Case 1 */
describe("Header Component", () => {
  const component = mount(<Header
    newDatasetClick={null}
    searchBoxChange={null}
    namespaceSelectChange={null}
    namespaces={[]}
    selectedNamespace={null}
  />);

  it("renders with correct styles", () => {
    expect(component).to.exist;
    expect(component.find(s.Header)).to.exist;
  });
});

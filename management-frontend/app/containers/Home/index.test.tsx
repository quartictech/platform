import * as React from "react";
import * as sinon from "sinon";
import { mount } from "enzyme";
import { expect } from "chai";
import { Home } from "./";

/** Data */
const props = {
  datasets: {},
  fetchDatasets: () => null
};

/** Spies */
const handleNavigate = sinon.spy(Home.prototype, "handleNavigate");

/** Case 1: Full Data */
describe("Home Container, Filled Data", () => {
  const component = mount(<Home {...props} />);

  it("renders NodeItem components with the same length of data", () => {
    expect(component.find("NodeItem")).to.have.length(1);
  });

  it("calls handleNavigate on nodeItem Click", () => {
    component.find({ name: "nodeItem" }).first().simulate("click");
    expect(handleNavigate).to.have.property("callCount", 1);
  });
});

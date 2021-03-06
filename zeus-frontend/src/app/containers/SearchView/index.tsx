import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";

import SearchContainer from "../../containers/SearchContainer";
import standardProviders from "../../containers/SearchContainer/standardProviders";

const s = require("./style.css");

// tslint:disable-next-line:variable-name
const SearchView: React.SFC<{}> = () => (
  <DocumentTitle title="Quartic - Search">
    <div className={s.container} style={{ marginTop: "10%" }}>
      <SearchContainer
        className={classNames(Classes.LARGE, Classes.ROUND, s.myPicker)}
        placeholder="What do you want to know?"
        providers={standardProviders}
      />
    </div>
  </DocumentTitle>
);

export default SearchView;

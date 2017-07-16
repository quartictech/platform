import * as React from "react";
import Header from "../../containers/Header";

const s = require("./style.css");

const App: React.SFC<{}> = props => (
  <div>
    <section className={s.App}>
      <Header />
      <div className={s.container}>
          <div className={s.main}>
            {props.children}
          </div>
      </div>
    </section>
  </div>
);

export default App;

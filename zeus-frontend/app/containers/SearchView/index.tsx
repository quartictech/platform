import * as React from "react";
import { connect } from "react-redux";
import {
} from "@blueprintjs/core";
import { createStructuredSelector } from "reselect";
import PredictingPicker from "../../components/PredictingPicker";
// import * as _ from "underscore";


interface IProps {
}

interface IState {
}

class SearchView extends React.Component<IProps, IState> {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div>
        <p>Hello mummy</p>
        <PredictingPicker
              iconName="search"
              entryIconName="person"
              placeholder="Search noobs..."
              
              entries={[
                {
                  key: "123",
                  name: "Arlo",
                  description: "CEO",
                  extra: "Arlo is a noob",
                  category: "Humans",
                },
                {
                  key: "456",
                  name: "Alex",
                  description: "CTO",
                  extra: "Alex is a noob",
                  category: "Humans",
                },
                {
                  key: "789",
                  name: "Oliver",
                  description: "Head of Engineering",
                  extra: "Oliver is a noob",
                  category: "Humans",
                },
                {
                  key: "ABC",
                  name: "Edmund",
                  description: "VP of Pugs",
                  extra: "Edmund is a noob",
                  category: "Animals",
                }
              ]}
              selectedKey={null}
              onChange={this.onNoobChange}
              errorDisabled={true}

        />
      </div>
    );
  }

  private onNoobChange(noob: string) {
    console.log("Selected:", noob);
  }
}

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchView);

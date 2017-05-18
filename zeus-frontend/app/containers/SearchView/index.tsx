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
              
              entries={{
                "123": {
                  name: "Arlo",
                  description: "CEO",
                  category: "Humans",
                },
                "456": {
                  name: "Alex",
                  description: "CTO",
                  category: "Humans",
                },
                "789": {
                  name: "Oliver",
                  description: "Head of Engineering",
                  category: "Humans",
                },
                "ABC": {
                  name: "Edmund",
                  description: "VP of Pugs",
                  category: "Animals",
                }
              }}
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

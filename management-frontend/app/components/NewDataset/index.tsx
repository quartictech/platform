import * as React from "react";

import { Dialog, Button, Intent } from "@blueprintjs/core";

import * as Dropzone from "react-dropzone";
import _ = require("underscore");

import { IDatasetMetadata } from "../../models";
import * as classNames from "classnames";

interface IFile {
  name: string;
}

interface INewDatasetProps {
  createDataset: (metadata: IDatasetMetadata, files: IFile[]) => any;
  visible: boolean;
  closeNewDatasetClick: any;
};

// NOTE: These are optional to make setState easier to call
interface IState {
    files?: IFile[];
    name?: string;
    description?: string;
};

const FileRow = ({ file }) => (
  <tr>
    <td>{file.name}</td>
    <td>{file.size}</td>
  </tr>
);

const FilesList = ({ files }) => (
  <table className="pt-table pt-striped" style={{width: "100%"}}>
    <tbody>
      { _.map(files, (file:IFile) => <FileRow key={file.name} file={file}/>) }
    </tbody>
  </table>
);

export class NewDataset extends React.Component<INewDatasetProps, IState> {
  constructor() {
    super();
    this.state = {
      name: "",
      description: "",
      files: []
    };
  }

  public onSave() {
    this.props.createDataset({
      name: this.state.name,
      description: this.state.description,
      attribution: "user data"
    }, this.state.files);
  }

  onDrop(files) {
    this.setState({ files });
  }

  onChangeName(e) {
    this.setState( { name: e.target.value });
  }

  onChangeDescription(e) {
    this.setState( { description: e.target.value });
  }

  toggleDialog() {
    this.props.closeNewDatasetClick();
  }

  isNameValid() {
    return this.state.name.length !== 0;
  }

  isDescriptionValid() {
    return this.state.description.length !== 0;
  }

  public render() {
    return (
      <Dialog
        iconName="inbox"
        isOpen={this.props.visible}
        onClose={this.toggleDialog.bind(this)}
        title="New Dataset"
        style={{backgroundColor:"#293742"}}
      >
        <div className="pt-dialog-body pt-dark">
          <label className="pt-label .modifier">
            Name
            <input
              className={classNames("pt-input", "pt-fill", {"pt-intent-danger": !this.isNameValid()})}
              type="text"
              placeholder="Name"
              dir="auto"
              onChange={this.onChangeName.bind(this)}
            />
          </label>

          <label className="pt-label .modifier">
          Description
            <textarea
              className={classNames("pt-input", "pt-fill", {"pt-intent-danger": !this.isDescriptionValid()})}
              type="text"
              placeholder="Description"
              dir="auto"
              onChange={this.onChangeDescription.bind(this)}
            />
          </label>

          <label className="pt-label .modifier">
            Files
            <Dropzone onDrop={this.onDrop.bind(this)} className="pt-card" style={{height: "100px", width: "100%"}}>
               {
                 this.state.files.length === 0 ?
                 <div>Try dropping some files here, or click to select files to upload.</div> :
                 <FilesList files={this.state.files}/>
               }
             </Dropzone>
           </label>
         </div>
         <div className="pt-dialog-footer">
           <div className="pt-dialog-footer-actions">
           <Button text="Cancel" onClick={this.toggleDialog.bind(this)} />
           <Button
             disabled={!(this.isNameValid() && this.isDescriptionValid())}
             intent={Intent.PRIMARY}
             onClick={this.onSave.bind(this)}
             text="Save"
           />
         </div>
       </div>
      </Dialog>
    );
  }
};

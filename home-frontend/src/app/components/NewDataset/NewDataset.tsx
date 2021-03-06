import * as React from "react";

import { Dialog, Button, Intent } from "@blueprintjs/core";

import * as Dropzone from "react-dropzone";
import _ = require("underscore");

import { IDatasetMetadata, IFiles } from "../../models";
import * as classNames from "classnames";

interface IFile {
  name: string;
}

interface INewDatasetProps {
  createDataset: (metadata: IDatasetMetadata, files: IFiles) => any;
  visible: boolean;
  closeNewDatasetClick: any;
}

// NOTE: These are optional to make setState easier to call
interface IState {
  files?: IFile[];
  name?: string;
  description?: string;
}

// tslint:disable-next-line:variable-name
const FileRow = ({ file }) => (
  <tr>
    <td style={{ wordWrap: "break-word" }}>{file.name}</td>
    <td style={{ width: "30%" }}>{file.size}</td>
  </tr>
);

// tslint:disable-next-line:variable-name
const FilesList = ({ files }) => (
  <table className="pt-table pt-striped" style={{ width: "100%", tableLayout: "fixed" }}>
    <tbody>
      {_.map(files, (file: IFile) => <FileRow key={file.name} file={file} />)}
    </tbody>
  </table>
);

export class NewDataset extends React.Component<INewDatasetProps, IState> {
  constructor() {
    super();
    this.state = {
      name: "",
      description: "",
      files: [],
    };

    this.toggleDialog = this.toggleDialog.bind(this);
    this.onChangeName = this.onChangeName.bind(this);
    this.onChangeDescription = this.onChangeDescription.bind(this);
    this.onDrop = this.onDrop.bind(this);
    this.onSave = this.onSave.bind(this);
  }

  public onSave() {
    this.props.createDataset(
      {
        name: this.state.name,
        description: this.state.description,
        attribution: "User data",
      },
      {
        files: this.state.files,
      },
    );
  }

  onDrop(files) {
    this.setState({ files });
  }

  onChangeName(e) {
    this.setState({ name: e.target.value });
  }

  onChangeDescription(e) {
    this.setState({ description: e.target.value });
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
        onClose={this.toggleDialog}
        title="New Dataset"
        style={{ backgroundColor: "#293742", minWidth: 400, width: "30%", bottom: "calc(100vh-50)" }}
      >
        <div className="pt-dialog-body pt-dark">
          <label className="pt-label">
            Name
            <input
              className={classNames("pt-input", "pt-fill", { "pt-intent-danger": !this.isNameValid() })}
              type="text"
              placeholder="Name"
              dir="auto"
              onChange={this.onChangeName}
            />
          </label>

          <label className="pt-label">
          Description
            <textarea
              className={classNames("pt-input", "pt-fill", { "pt-intent-danger": !this.isDescriptionValid() })}
              type="text"
              placeholder="Description"
              dir="auto"
              onChange={this.onChangeDescription}
            />
          </label>

          <label className="pt-label">
            Files
            <Dropzone
              disableClick={true}
              onDrop={this.onDrop}
              className="pt-card"
              style={{ height: 100, width: "100%" }}
            >
              {this.filesListOrMessage()}
            </Dropzone>
          </label>
         </div>
         <div className="pt-dialog-footer">
           <div className="pt-dialog-footer-actions">
           <Button text="Cancel" onClick={this.toggleDialog} />
           <Button
             disabled={!(this.isNameValid() && this.isDescriptionValid())}
             intent={Intent.PRIMARY}
             onClick={this.onSave}
             text="Save"
           />
         </div>
       </div>
      </Dialog>
    );
  }

  private filesListOrMessage() {
    return (this.state.files.length === 0)
      ? <div>Drag files here, or click to select files to upload.</div>
      : <FilesList files={this.state.files} />;
  }
}

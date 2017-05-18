// TODO - this entire component is duplicated from platform :(
import * as React from "react";
import {
  Classes,
  Colors,
  IconContents,
  InputGroup,
  Intent,
  Keys,
  Menu,
  MenuDivider,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  Position,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";

// TODO: look into https://basarat.gitbooks.io/typescript/docs/types/index-signatures.html

// TODO: should these all be optional?
// TODO: could we just pass a bunch of these through implicitly?
interface PredictingPickerProps {
  type?: string;
  iconName?: string;
  entryIconName?: string;
  placeholder?: string;
  entries: any; // TODO: is this type correct?
  selectedKey: string;
  disabled?: boolean;
  errorDisabled?: boolean;
  onChange?: (key: string) => void;
}

interface PredictingPickerState {
  text: string;
  menuVisible: boolean;
  shouldFilter: boolean;
  idxHighlighted: number;
  mouseCaptured: boolean;
}

// TODO: add scrolling (see https://github.com/palantir/blueprint/pull/1049)

export default class PredictingPicker extends React.Component<PredictingPickerProps, PredictingPickerState> {
  public static defaultProps: Partial<PredictingPickerProps> = {
    errorDisabled: false,
    disabled: false,
  };

  public constructor(props) {
    super(props);
    this.state = {
      text: "",
      menuVisible: false,
      shouldFilter: false,
      idxHighlighted: 0,
      mouseCaptured: false,
    };

    // TODO: do we still need these in TS?
    this.onInteraction = this.onInteraction.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
  }

  public componentWillReceiveProps(nextProps: PredictingPickerProps) {
    if (nextProps.selectedKey && (this.props.selectedKey !== nextProps.selectedKey)) {
      this.setState({ text: this.entriesAsMap()[nextProps.selectedKey] });
    }
  }

  public render() {
    return (
      <Popover
        autoFocus={false}
        enforceFocus={false}
        popoverClassName={Classes.MINIMAL}
        content={this.renderMenu()}
        isOpen={!this.props.disabled && this.state.menuVisible}
        onInteraction={this.onInteraction}
        interactionKind={PopoverInteractionKind.CLICK}
        position={Position.BOTTOM_LEFT}
      >
        <InputGroup
          disabled={this.props.disabled}
          type={this.props.type}
          leftIconName={this.props.iconName || this.props.entryIconName}
          placeholder={this.props.placeholder}
          value={this.state.text}
          onKeyDown={(e) => this.onKeyDown(e)}
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={(this.props.selectedKey || this.props.disabled || this.props.errorDisabled) ? Intent.NONE : Intent.DANGER}
        />
      </Popover>
    );
  }

  private renderMenu() {
    const items = _.map(this.categorisedFilteredEntries() as _.Dictionary<any>, (entries, category) => this.renderCategory(category, entries));

    return (
      <Menu>
        {
          _.isEmpty(items)
            ? <MenuItem className={classNames(Classes.MENU_ITEM, Classes.DISABLED)} text="No results found." />
            : items
        }
      </Menu>
    );
  }

  private renderCategory(category: string, entries: Map<string, string>) {
    return (
      <div key={category}>
        {(category !== "undefined") && <MenuDivider title={category} />}
        {_.map(entries as _.Dictionary<any>, entry => this.renderEntry(entry))}
      </div>
    );
  }

  // 25px is a hack - compensates for hardcoded ::before size in Blueprint CSS
  private renderEntry(entry) {
    return (
      <div
        key={entry.key}
        style={(entry.idx === this.state.idxHighlighted) ? { backgroundColor: Colors.BLUE3 } : {}}  // TODO: set ::before color to white
        onMouseEnter={() => this.onMouseEnter(entry.idx)}
        onMouseLeave={() => this.onMouseLeave(entry.idx)}
      >
        <MenuItem
          key={entry.key}
          text={(
            <div style={{ marginLeft: "25px" }}>
              <div><b>{entry.name}</b></div>
              <small className="pt-text-muted">
                {
                  entry.extra
                    ? (
                    <div>
                      <p><b>{entry.description}</b></p>
                      <div style={{ textAlign: "right" }}>
                        <em>{entry.extra}</em>
                      </div>
                    </div>
                    )
                    : (
                    <b>{entry.description}</b>
                    )
                }

              </small>
            </div>
          ) as any} // Cast required because text not declared as string | JSXElement (even though that works)
          label={(this.props.selectedKey === entry.key) ? IconContents.TICK : ""}
          iconName={this.props.entryIconName}
          onClick={() => this.onSelectEntry(entry.key)}
        />
      </div>
    );
  }

  // We cannot prevent the Blueprint :hover behaviour, thus if the mouse is currently hovered over an entry we have
  // to disable the up/down arrow behaviour.  This is what mouseCaptured is for.
  private onMouseEnter(idx: number) {
    this.setState({ mouseCaptured: true, idxHighlighted: idx });
  }

  private onMouseLeave(_: number) {
    this.setState({ mouseCaptured: false });
  }

  private onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    switch (e.which) {
      case Keys.ARROW_DOWN:
        if (!this.state.mouseCaptured) {
          this.setState({ idxHighlighted: Math.min(_.size(this.filteredEntries()) - 1, this.state.idxHighlighted + 1) });
        }
        e.preventDefault();
        break;
      case Keys.ARROW_UP:
        if (!this.state.mouseCaptured) {
          this.setState({ idxHighlighted: Math.max(0, this.state.idxHighlighted - 1) });
        }
        e.preventDefault();
        break;
      case Keys.ENTER: {
        const highlighted = _.chain(this.categorisedFilteredEntries())
          .values()
          .flatten()
          .find(entry => entry.idx === this.state.idxHighlighted)
          .value();
        if (highlighted) {
          this.onSelectEntry(highlighted.key);
        }
        e.preventDefault();
        break;
      }
      default:
        break;
    }
  }

  private onSelectEntry(key: string) {
    this.setState({ menuVisible: false });
    this.props.onChange(key);
  }

  private onInteraction(nextOpenState: boolean) {
    this.setState({
      menuVisible: nextOpenState,
      shouldFilter: false,
      idxHighlighted: 0,
      mouseCaptured: false,
    });
  }

  private onChangeText(text: string) {
    this.setState({
      text,
      menuVisible: true,
      shouldFilter: true,
      idxHighlighted: 0,
      mouseCaptured: false,
    });
    this.props.onChange(_.invert(this.entriesAsMap())[text]);
  }

  private categorisedFilteredEntries() {
    let idx = 0;
    return _.chain(this.filteredEntries())
      .groupBy(entry => entry.category)
      .mapObject(entries => _.map(entries, entry => ({ ...entry, idx: idx++ })))
      .value();
  }

  private filteredEntries() {
    return _.chain(this.entriesAsMap())
      .map((v, k: string) =>  this.normalize(k, v))
      .values()
      .filter(entry => !this.state.shouldFilter || !this.state.text || entry.name.toLowerCase().includes(this.state.text.toLowerCase()))
      .value();
  }

  // TODO: this type stuff is gross
  private entriesAsMap() {
    return _.isArray(this.props.entries)
      ? _.object(_.map(this.props.entries as Array<any>, x => [x, x]))
      : this.props.entries;
  }

  private normalize = (key: string, entry: any) => {
    const isObject = (typeof entry === "object");
    return {
      key,
      name: isObject ? entry.name : entry,
      description: isObject ? entry.description : undefined,
      extra: isObject ? entry.extra : undefined,
      category: isObject ? entry.category : undefined,
    };
  };
}
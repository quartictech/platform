// TODO - this entire component is duplicated from platform :(
import * as React from "react";
import {
  Classes,
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
  Spinner,
  Utils as BlueprintUtils,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";
const s = require("./style.css");

export interface PickerEntry {
  key: string;
  name: string;
  description?: string;
  extra?: string;
  category?: string;
  iconName?: string;
}

interface NumberedEntry {
  idx: number;
  entry: PickerEntry;
}

interface CategorisedEntries {
  [index: string] : NumberedEntry[];
}

export interface PickerProps {
  className?: string;
  type?: string;
  iconName?: string;
  defaultEntryIconName?: string;
  placeholder?: string;
  entries: PickerEntry[];
  selectedKey: string;
  disabled?: boolean;
  errorDisabled?: boolean;
  working?: boolean;
  onEntrySelect?: (entry: PickerEntry) => void;
  onQueryChange?: (text: string) => void;
  manageFilter?: boolean; // If true, then onQueryChange is ignored
}

interface PickerState {
  text: string;
  menuVisible: boolean;
  idxHighlighted: number;
  categorisedEntries: CategorisedEntries;  // Cached to avoid recompute during render
}

export default class Picker extends React.Component<PickerProps, PickerState> {
  public static defaultProps: Partial<PickerProps> = {
    errorDisabled: false,
    disabled: false,
  };

  public constructor(props) {
    super(props);
    this.state = {
      text: "",               // Text to be displayed in search box
      menuVisible: false,
      idxHighlighted: 0,
      categorisedEntries: null,
    };

    this.onInteraction = this.onInteraction.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
  }

  public componentWillMount() {
    this.recalculateCategorisedEntries(this.props.entries, this.state.text);
  }

  public componentWillReceiveProps(nextProps: PickerProps) {
    // TODO: controlled selection

    // // The selection can be affected either by either (1) what the user types into the search box, or (2) by
    // // explicitly selecting something in the menu.  The mechanism below ensure the search-box text is updated in case
    // // #2 (assuming a feedback mechanism from onChange to selectedKey outside this component).
    // // However, in case #1 there's the case where the user types something that doesn't match any entry, leading to
    // // key being null.  Hence the logic that skips the update in that case.
    // if (nextProps.selectedKey && (this.props.selectedKey !== nextProps.selectedKey)) {
    //   this.setState({ text: this.props.entries[nextProps.selectedKey].name });
    // }

    // Note: this is a shallow check
    if (this.props.entries !== nextProps.entries) {
      this.recalculateCategorisedEntries(nextProps.entries, this.state.text);
      this.resetHighlight();
    }
  }

  private recalculateCategorisedEntries(entries: PickerEntry[], text: string) {
    let idx = 0;
    // TODO: make types work properly
    const categorisedEntries: {} = _.chain(entries)
      .filter(x => (this.props.manageFilter ? matches(x, text) : true))
      .groupBy(entry => entry.category)
      .map((entries, category: string) => [category, _.map(entries, entry => ({idx: idx++, entry} as NumberedEntry))])
      .object()
      .value();
    this.setState({ categorisedEntries });
  }

  private onMouseEnter(idx: number) {
    this.setHighlightIndex(idx);
  }

  private onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    switch (e.which) {
      case Keys.ARROW_DOWN:
        this.setHighlightIndex(this.state.idxHighlighted + 1);
        break;

      case Keys.ARROW_UP:
        this.setHighlightIndex(this.state.idxHighlighted - 1);
        break;

      case Keys.ENTER: {
        const highlighted = this.getHighlightedEntry();
        if (highlighted) {
          this.onSelectEntry(highlighted.key);
        }
        break;
      }

      case Keys.ESCAPE: {
        this.onChangeText("");
        break;
      }

      default:
        return;
    }

    e.preventDefault();
  }

  private onSelectEntry(key: string) {
    this.hideMenu();
    BlueprintUtils.safeInvoke(this.props.onEntrySelect, _.find(this.props.entries, e => e.key === key));
  }

  private onInteraction(nextOpenState: boolean) {
    nextOpenState ? this.showMenu() : this.hideMenu();
    this.resetHighlight();
  }

  private onChangeText(text: string) {
    this.setState({ text });
    this.showMenu();
    this.resetHighlight();

    if (this.props.manageFilter) {
      this.recalculateCategorisedEntries(this.props.entries, text);
    } else {
      BlueprintUtils.safeInvoke(this.props.onQueryChange, text);
    }
  }

  private resetHighlight() {
    this.setHighlightIndex(0);
  }

  private setHighlightIndex(idx: number) {
    const clamped = Math.min(_.size(this.props.entries) - 1, Math.max(0, idx));
    this.setState({ idxHighlighted: clamped });
  }

  private showMenu() {
    this.setState({ menuVisible: true });
  }

  private hideMenu() {
    this.setState({ menuVisible: false });
  }

  private getHighlightedEntry(): PickerEntry {
    return _.chain(this.state.categorisedEntries)
          .values()
          .flatten()
          .find(entry => entry.idx === this.state.idxHighlighted)
          .value()
          .entry;
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
          className={this.props.className}
          disabled={this.props.disabled}
          type={this.props.type}
          leftIconName={this.props.iconName || this.props.defaultEntryIconName}
          rightElement={(this.props.working && this.state.menuVisible)
            ? <Spinner className={Classes.SMALL} />
            : undefined
          }
          placeholder={this.props.placeholder}
          value={this.state.text}
          onKeyDown={(e) => this.onKeyDown(e)}
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={
            (this.props.selectedKey || this.props.disabled || this.props.errorDisabled) ? Intent.NONE : Intent.DANGER
          }
        />
      </Popover>
    );
  }

  private renderMenu() {
    const items =
      _.map(this.state.categorisedEntries, (entries, category: string) => this.renderCategory(category, entries));

    if (_.isEmpty(items) && this.state.text === "") {
      return null;  // Otherwise a weird empty box appears
    }

    return (
      <Menu className={this.props.className}>
        {
          (_.isEmpty(items) && this.state.text !== "")
            ? <MenuItem className={classNames(Classes.MENU_ITEM, Classes.DISABLED)} text="No results found." />
            : items
        }
      </Menu>
    );
  }

  private renderCategory(category: string, entries: NumberedEntry[]) {
    return (
      <div key={category}>
        {(category !== "undefined") && <MenuDivider title={category} />}
        {_.map(entries, entry => this.renderEntry(entry.entry, entry.idx))}
      </div>
    );
  }

  // marginLeft is a hack - compensates for hardcoded ::before size in Blueprint CSS.
  // Note that the MenuItem behaviour is not very controllable, so we completely override it - we've disabled 
  // pointer-events, and use a wrapper div to capture events and do colouring.  This is gross.
  private renderEntry(entry: PickerEntry, idx: number) {
    const isHighlighted = (idx === this.state.idxHighlighted);
    return (
      <div
        key={entry.key}
        className={isHighlighted ? s.highlighted : null}
        style={{ cursor: "pointer" }}
        onMouseEnter={() => this.onMouseEnter(idx)}
        onClick={() => this.onSelectEntry(entry.key)}
      >
        <MenuItem
          key={entry.key}
          className={s.bad}
          text={(
            <div style={{ marginLeft: "30px" }}>
              <div><b>{entry.name}</b></div>
              <small className={isHighlighted ? null : "pt-text-muted"}>
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
          iconName={entry.iconName || this.props.defaultEntryIconName}
        />
      </div>
    );
  }
}

const matches = (entry: PickerEntry, text: string) =>
  stringInString(text, entry.name) || stringInString(text, entry.description) || stringInString(text, entry.extra);


const stringInString = (needle: string, haystack: string) =>
  haystack && (haystack.toLocaleLowerCase().indexOf(needle.toLocaleLowerCase()) !== -1);


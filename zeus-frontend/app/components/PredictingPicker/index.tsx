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
const s = require("./style.css");

// TODO: sort out inverse text colouring when selected
// TODO: sort out bold font weight
// TODO: look into https://basarat.gitbooks.io/typescript/docs/types/index-signatures.html
// TODO: add scrolling (see https://github.com/palantir/blueprint/pull/1049)
// TODO: add back convenience support for array of string entries
// TODO: spinner?

interface PredictingPickerEntry {
  key: string;
  name: string;
  description?: string;
  extra?: string;
  category?: string;
}

interface NumberedEntry {
  idx: number;
  entry: PredictingPickerEntry;
}

interface CategorisedEntries {
  [index: string] : NumberedEntry[]
}

// TODO: should these all be optional?
// TODO: could we just pass a bunch of these through implicitly?
interface PredictingPickerProps {
  type?: string;
  iconName?: string;
  entryIconName?: string;
  placeholder?: string;
  entries: PredictingPickerEntry[];
  selectedKey: string;
  disabled?: boolean;
  errorDisabled?: boolean;
  onChange?: (key: string) => void;
}

interface PredictingPickerState {
  text: string;
  menuVisible: boolean;
  idxHighlighted: number;
  sortedEntries: CategorisedEntries;  // Cached to avoid recompute during render
}

export default class PredictingPicker extends React.Component<PredictingPickerProps, PredictingPickerState> {
  public static defaultProps: Partial<PredictingPickerProps> = {
    errorDisabled: false,
    disabled: false,
  };

  public constructor(props) {
    super(props);
    this.state = {
      text: "",               // Text to be displayed in search box
      menuVisible: false,
      idxHighlighted: 0,
      sortedEntries: null,
    };

    // TODO: do we still need these in TS?
    this.onInteraction = this.onInteraction.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
  }

  public componentWillMount() {
    this.recalculateSortedEntries(this.props.entries);
  }

  public componentWillReceiveProps(nextProps: PredictingPickerProps) {
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
      this.recalculateSortedEntries(nextProps.entries);
      this.resetHighlight();
    }
  }

  private recalculateSortedEntries(entries: PredictingPickerEntry[]) {
    let idx = 0;
    // TODO: make types work properly
    const sortedEntries: {} = _.chain(entries)
      .groupBy(entry => entry.category)
      .map((entries, category: string) => [category, _.map(entries, entry => ({idx: idx++, entry} as NumberedEntry))])
      .object()
      .value();
    this.setState({ sortedEntries });
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

      default:
        return;
    }

    e.preventDefault();
  }

  private onSelectEntry(key: string) {
    this.hideMenu();
    this.props.onChange(key);
  }

  private onInteraction(nextOpenState: boolean) {
    nextOpenState ? this.showMenu() : this.hideMenu();
    this.resetHighlight();
  }

  private onChangeText(text: string) {
    this.setState({ text });
    this.showMenu();
    this.resetHighlight();

    const matchingEntry = _.find(this.props.entries, entry => stringInString(text, entry.name));
    this.props.onChange(matchingEntry ? matchingEntry.key : undefined); // TODO: is this nice?
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

  private getHighlightedEntry() {
    return _.chain(this.state.sortedEntries)
          .values()
          .flatten()
          .find(entry => entry.idx === this.state.idxHighlighted)
          .value();
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
    const items = _.map(this.state.sortedEntries, (entries, category: string) => this.renderCategory(category, entries));

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

  private renderCategory(category: string, entries: NumberedEntry[]) {
    return (
      <div key={category}>
        {(category !== undefined) && <MenuDivider title={category} />}
        {_.map(entries, entry => this.renderEntry(entry.entry, entry.idx))}
      </div>
    );
  }

  // 25px is a hack - compensates for hardcoded ::before size in Blueprint CSS.
  // Note that the MenuItem behaviour is not very controllable, so we completely override it - we've disabled 
  // pointer-events, and use a wrapper div to capture events and do colouring.
  private renderEntry(entry: PredictingPickerEntry, idx: number) {
    const isHighlighted = (idx === this.state.idxHighlighted);
    return (
      <div
        key={entry.key}
        style={{
          backgroundColor: isHighlighted ? Colors.BLUE3 : null,
          cursor: "pointer"
        }}  // TODO: set ::before color to white
        onMouseEnter={() => this.onMouseEnter(idx)}
        onClick={() => this.onSelectEntry(entry.key)}
      >
        <MenuItem
          className={s.bad}
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
        />
      </div>
    );
  }
}

function stringInString(needle: string, haystack: string) {
  return haystack.toLowerCase().includes(needle.toLowerCase());
}



// TODO: move this logic to external
// TODO: shouldFilter is false when first interacted with, switches to true as soon as typing 

  // private filteredEntries(): PredictingPickerEntry[] {
  //   // No filtering if disabled or if there's no text to filter by!
  //   if (!this.state.shouldFilter || !this.state.text) {
  //     return this.props.entries;
  //   }

  //   return _.chain(this.props.entries)
  //     .filter(entry => this.stringInString(this.state.text, entry.name))
  //     .value();
  // }

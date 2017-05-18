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

// TODO: sort out inverse text colouring when selected
// TODO: sort out bold font weight
// TODO: look into https://basarat.gitbooks.io/typescript/docs/types/index-signatures.html
// TODO: add scrolling (see https://github.com/palantir/blueprint/pull/1049)
// TODO: add back convenience support for array of string entries


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
  shouldFilter: boolean;
  idxHighlighted: number;
  mouseCaptured: boolean;
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
    // The selection can be affected either by either (1) what the user types into the search box, or (2) by
    // explicitly selecting something in the menu.  The mechanism below ensure the search-box text is updated in case
    // #2 (assuming a feedback mechanism from onChange to selectedKey outside this component).
    // However, in case #1 there's the case where the user types something that doesn't match any entry, leading to
    // key being null.  Hence the logic that skips the update in that case.
    if (nextProps.selectedKey && (this.props.selectedKey !== nextProps.selectedKey)) {
      this.setState({ text: this.props.entries[nextProps.selectedKey].name });
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
    const items = _.map(this.categorisedFilteredEntries(), (entries, category: string) => this.renderCategory(category, entries));

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
        {(category !== "undefined") && <MenuDivider title={category} />}
        {_.map(entries, entry => this.renderEntry(entry.entry, entry.idx))}
      </div>
    );
  }

  // 25px is a hack - compensates for hardcoded ::before size in Blueprint CSS
  private renderEntry(entry: PredictingPickerEntry, idx: number) {
    return (
      <div
        key={entry.key}
        style={(idx === this.state.idxHighlighted) ? { backgroundColor: Colors.BLUE3 } : {}}  // TODO: set ::before color to white
        onMouseEnter={() => this.onMouseEnter(idx)}
        onMouseLeave={() => this.onMouseLeave(idx)}
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

    const matchingEntry = _.find(this.props.entries, entry => this.stringInString(text, entry.name));
    this.props.onChange(matchingEntry.key);
  }

  private categorisedFilteredEntries(): CategorisedEntries {
    let idx = 0;
    // TODO: make types work properly
    const x: {} = _.chain(this.filteredEntries())
      .groupBy(entry => entry.category)
      .map((entries, category: string) => [category, _.map(entries, entry => ({idx: idx++, entry} as NumberedEntry))])
      .object()
      .value();
    return x;
  }

  private filteredEntries(): PredictingPickerEntry[] {
    // No filtering if disabled or if there's no text to filter by!
    if (!this.state.shouldFilter || !this.state.text) {
      return this.props.entries;
    }

    return _.chain(this.props.entries)
      .filter(entry => this.stringInString(this.state.text, entry.name))
      .value();
  }

  private stringInString(needle: string, haystack: string) {
    return haystack.toLowerCase().includes(needle.toLowerCase());
  }
}
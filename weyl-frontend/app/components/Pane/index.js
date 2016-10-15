import React from "react";
import styles from "./styles.css";

// HACK: For whatever reason, we don't get a pointer cursor on hover without this
const nullLink = "javascript:;";  // eslint-disable-line no-script-url

const Pane = ({
  title,
  visible,
  onClose,
  children,
}) => (
  <div className={styles.pane} style={{ "visibility": visible ? "visible" : "hidden" }}>
    <div className="ui raised fluid segment">
      {
        title && (
          <h4 className="ui header">
            <a href={nullLink}>
              <i className="icon close" onClick={onClose}></i>
            </a>
            {title}
          </h4>
        )
      }
      {children}
    </div>
  </div>
);

export default Pane;

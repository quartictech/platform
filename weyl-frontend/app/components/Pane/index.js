import React from "react";
import styles from "./styles.css";

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
            <a href="">
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

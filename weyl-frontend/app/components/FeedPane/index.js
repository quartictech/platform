import React from "react";
import styles from "./styles.css";

function FeedPane() {
  return (
    <div className={styles.feedPane}>
      <div className="ui raised fluid card">
        <div className="content">
          <div className="header">
            <a>
              <i className="icon close"></i>
            </a>
            Live feed
          </div>
          <div className="meta">
            Random BS
          </div>

          <div className="ui small feed">
            <div className="event">
              <div className="label">
                <i className="circular blue twitter icon"></i>
              </div>

              <div className="content">
              <div className="date">1 hour ago</div>
                <div className="summary">
                  <a className="user">Oliver Charlesworth</a> spunked in your face
                </div>

                <div className="extra text">
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                </div>
              </div>
            </div>

            <div className="event">
              <div className="label">
                <i className="circular blue facebook icon"></i>
              </div>

              <div className="content">
              <div className="date">2 hours ago</div>
                <div className="summary">
                  <a className="user">Arlo Bryer</a> started a dirty protest
                </div>

                <div className="extra text">
                  Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                </div>
              </div>
            </div>

            <div className="event">
              <div className="label">
                <i className="circular red reddit icon"></i>
              </div>

              <div className="content">
              <div className="date">3 hours ago</div>
                <div className="summary">
                  <a className="user">Alex Sparrow</a> noobed
                </div>

                <div className="extra text">
                  Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

export default FeedPane;

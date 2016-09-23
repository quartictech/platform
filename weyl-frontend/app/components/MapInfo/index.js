/**
*
* MapInfo
*
*/

import React from 'react';

import styles from './styles.css';

function MapInfo({
  map
}) {
  return (
    <div className={styles.mapInfo}>
      {
        (map.mouseLocation !== null) ? `${map.mouseLocation.lng.toFixed(4)}, ${map.mouseLocation.lat.toFixed(4)}` : ""
      }
    </div>
  );
}

export default MapInfo;

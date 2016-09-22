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
  console.log(map);
  return (
    <div className={styles.mapInfo}>
      {
        (map.mouseLocation !== null) ? `${map.mouseLocation.lng.toFixed(4)}, ${map.mouseLocation.lat.toFixed(4)}` : ""
      }
    </div>
  );
}

export default MapInfo;

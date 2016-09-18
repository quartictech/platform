
let line_colors = [
"#003688",
"#E51836",
"#A0A5A9",
"#95CDBA",
"#0098D4",
"#9B0056",
"#84B817",
"#F3A9BB",
"#B36305",
"#000000",
"#FFD300",
"#7156A5",
"#E32017",
"#00A4A7",
"#EE7C0E",
"#00782A"]

let tube_color_stops = [
  ["Bakerloo","#B36305"],
["Central","#E32017"],
["Circle","#FFD300"],
["Crossrail","#7156A5"],
["District","#00782A"],
["DLR","#00A4A7"],
["Emirates Air Line","#E51836"],
["Hammersmith & City","#F3A9BB"],
["Jubilee","#A0A5A9"],
["London Overground","#EE7C0E"],
["Metropolitan","#9B0056"],
["Northern","#000000"],
["Piccadilly","#003688"],
["Tramlink","#84B817"],
["Victoria","#0098D4"],
["Waterloo & City","#95CDBA"]
]


let line_color_stops = []
for (var color of line_colors) {
    line_color_stops.push([color, color]);
}

export { line_color_stops, tube_color_stops }

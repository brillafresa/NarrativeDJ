(function () {
  "use strict";

  function renderSchedule(schedule) {
    var tbody = document.getElementById("scheduleRows");
    tbody.innerHTML = "";
    schedule.locations.forEach(function (loc) {
      var row = document.createElement("tr");
      row.innerHTML =
        "<td>" + loc.name + "<br><span class='badge'>" + loc.location_id + "</span></td>" +
        "<td>" + loc.profile_id + "</td>" +
        "<td>" + loc.active_hours + "</td>" +
        "<td>" + loc.timezone + "</td>";
      tbody.appendChild(row);
    });
    document.getElementById("status").textContent =
      "Loaded " + schedule.locations.length + " location(s)";
    if (window.NarrativeDJAdmin && window.NarrativeDJAdmin.postMessage) {
      window.NarrativeDJAdmin.postMessage(JSON.stringify({ event: "schedule_loaded", count: schedule.locations.length }));
    }
  }

  fetch("default_schedule.json")
    .then(function (res) { return res.json(); })
    .then(renderSchedule)
    .catch(function (err) {
      document.getElementById("status").textContent = "Failed to load schedule: " + err;
    });
})();

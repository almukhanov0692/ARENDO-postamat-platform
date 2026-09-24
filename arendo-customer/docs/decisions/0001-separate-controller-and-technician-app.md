# ADR 0001: Separate controller and technician application

- Status: Accepted
- Date: 2026-09-24

## Decision

Maintain the postamat controller and the Android technician application as separate products connected through a versioned physical USB protocol.

## Reason

The controller must continue server communication, local enforcement, event storage and media playback without the technician phone. The technician application is authorized for specific postamats and performs state-changing service actions only while physically attached.

## Consequences

- UI changes cannot alter controller behavior directly.
- Controller implementation details are not exposed in the technician app.
- USB compatibility and security require dedicated testing.
- A mock USB transport is required for development before hardware is finalized.


package com.cinekeep.list;

import jakarta.validation.constraints.Size;

public record UpdateListItemRequest(@Size(max = 1000) String comment) {
}

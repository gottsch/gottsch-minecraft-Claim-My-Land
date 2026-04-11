/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 *
 */

package mod.gottsch.neo.claimmyland.core.command;

/**
 * a simple result wrapper that distinguishes between success, empty (not needed),
 * and failure (validation error).
 *
 * @author by Mark Gottschling on 3/2/2026
 */
public class Result<T> {
    public enum Status { SUCCESS, EMPTY, FAILURE }

    private final Status status;
    private final T value;

    private Result(Status status, T value) {
        this.status = status;
        this.value = value;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(Status.SUCCESS, value);
    }

    public static <T> Result<T> empty() {
        return new Result<>(Status.EMPTY, null);
    }

    public static <T> Result<T> failure() {
        return new Result<>(Status.FAILURE, null);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isEmpty()   { return status == Status.EMPTY; }
    public boolean isFailure() { return status == Status.FAILURE; }

    public T getValue() { return value; }
}

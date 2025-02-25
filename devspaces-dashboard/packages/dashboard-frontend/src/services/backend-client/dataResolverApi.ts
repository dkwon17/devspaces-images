/*
 * Copyright (c) 2018-2023 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */

import { helpers } from '@eclipse-che/common';
import axios from 'axios';

import { dashboardBackendPrefix } from '@/services/backend-client/const';

export async function getDataResolver<T>(location: string): Promise<T> {
  try {
    const response = await axios.post<T>(`${dashboardBackendPrefix}/data/resolver`, {
      url: location,
    });

    return response?.data;
  } catch (e) {
    throw new Error(`Failed to resolve data. ${helpers.errors.getMessage(e)}`);
  }
}

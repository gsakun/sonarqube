/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { ClipboardIconButton, CodeSnippet, NumberedListItem } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { InlineSnippet } from './InlineSnippet';

export interface CreateYmlFileProps {
  yamlFileName: string;
  yamlTemplate: string;
}

export default function CreateYmlFile(props: CreateYmlFileProps) {
  const { yamlTemplate, yamlFileName } = props;
  return (
    <NumberedListItem>
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.github_action.yaml.create_yml')}
        id="onboarding.tutorial.with.github_action.yaml.create_yml"
        values={{
          file: (
            <>
              <InlineSnippet snippet={yamlFileName} />
              <ClipboardIconButton copyValue={yamlFileName} />
            </>
          ),
        }}
      />
      <CodeSnippet className="sw-p-6 sw-overflow-auto" snippet={yamlTemplate} language="yml" />
    </NumberedListItem>
  );
}

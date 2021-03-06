/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FormsMetadataTest {

    @Test
    public void canReadSimpleMetadaFile() {
        FormsMetadata metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/metadata.json");

        assertThat(metadata.getAlerts()).containsExactly("foo", "bar");
        assertThat(metadata.getBlocks()).hasSize(2);
        assertThat(metadata.getBlocks()[0].getName()).isEqualTo("foo");
        assertThat(metadata.getBlocks()[0].getItems()).containsExactly("item1", "item2");
        assertThat(metadata.getBlocks()[1].getName()).isEqualTo("bar");
        assertThat(metadata.getBlocks()[1].getItems()).containsExactly("item1", "item2");
        assertThat(metadata.getLovs()).containsExactly("foo", "bar");
    }


    @Test
    public void returnsNullIfFileDoesNotExists() {
        FormsMetadata metadata = FormsMetadata.loadFromFile("src/test/resources/metadata/metadata2.json");

        assertThat(metadata).isNull();
    }

}

# REDHAWK Consolidated Core Asset Library

## Description
This repository contains a consolidation of the REDHAWK core assets. The CI build is performed by the .gitlab-ci.yml file in the root directory of this repository. This file needs to be generated when assets are added to the repository or a new version is created.

To generate a new .gitlab-ci.yml file, run generate_gitlab.py. This script needs to be updated with release information (supported operating systems and version number). The file gitlab-ci.yml.template is used by generate_gitlab.py and contains information like the job and anchor descriptions.

## Copyrights

Each asset in this work is protected by Copyright. Please refer to the corresponding [Copyright File](COPYRIGHT) for updated copyright information.                                                                                                                

## License

The REDHAWK Core Assets are licensed under the GNU Lesser General Public License (LGPL).

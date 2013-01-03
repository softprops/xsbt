=========
Resolvers
=========

Overview
--------

Resolvers are URI's used for resolving the locations of remote library dependencies and targets for publishing artifacts. Resolvers in sbt come in many flavors. This section helps outline the most common flavors and how to use them in your project.

Sbt defines many utilities for building typed resolvers that represent the different kinds of resolvers sbt supports. In practice, you will only need to use a few common ones provided by the ``Opts.resolver`` object. Below you will learn about those as well as how to define your own.

Resolver Build Settings
-----------------

The most common build settings that use resolvers are ``publish-to`` and ``resolvers``. You can find out more about those settings using the ``help`` command and inspect their values in your build with the ``show`` command.

Maven
-----

Resolvers for Maven2 repositories are added as follows:

``scala resolvers += Some(Opts.resolver.sonatypeSnapshots)

This is the most common kind of user-defined resolver. The rest of this
page describes how to define other types of repositories.


Opts.resolver
-------------

Opts.resolver is an object that provides commonly used resolvers as constants.

- ``Opts.resolver.sonatypeReleases`` For publishing snapshots to the sonatype OSS repository
- ``Opts.resolver.sonatypeSnapshots`` For publishing release versions to the sonatype OSS repository
- ``Opts.resolver.sonatypeStaging`` For publishing to Maven-style artifacts to the sonatype OSS repository
- ``Opts.resolver.mavenLocalFile`` For publishing to your systems local Maven repository

Predefined
----------

The Opts.resolver object defined constants are implemented using resolver builders with predefined resolver respositories.

For instance, Opts.resolver.sonatypeReleases is defined using ``Resolver.sonatypeRepo("releases")``, which in turn is defined using

::

    def sonatypeRepo(status: String) = new MavenRepository("sonatype-" + status, SonatypeRepositoryRoot + "/" + status)

A few predefined repositories are available and are listed below

-  ``DefaultMavenRepository`` This is the main Maven repository at
   http://repo1.maven.org/maven2/ and is included by default
-  ``JavaNet1Repository`` This is the Maven 1 repository at
   http://download.java.net/maven/1/

For example, to use the ``java.net`` repository, use the following
setting in your build definition:

::

    resolvers += JavaNet1Repository

Predefined repositories will go under Resolver going forward so they are
in one place:

::

    Resolver.sonatypeRepo("releases")  // Or "snapshots"

Custom
------

sbt provides an interface to the repository types available in Ivy:
file, URL, SSH, and SFTP. A key feature of repositories in Ivy is using
`patterns <http://ant.apache.org/ivy/history/latest-milestone/concept.html#patterns>`_
to configure repositories.

Construct a repository definition using the factory in ``sbt.Resolver``
for the desired type. This factory creates a ``Repository`` object that
can be further configured. The following table contains links to the Ivy
documentation for the repository type and the API documentation for the
factory and repository class. The SSH and SFTP repositories are
configured identically except for the name of the factory. Use
``Resolver.ssh`` for SSH and ``Resolver.sftp`` for SFTP.

.. _Ivy filesystem: http://ant.apache.org/ivy/history/latest-milestone/resolver/filesystem.html
.. _filesystem factory: ../../api/sbt/Resolver$$file$.html
.. _Ivy sftp: http://ant.apache.org/ivy/history/latest-milestone/resolver/sftp.html
.. _FileRepository API: ../../api/sbt/FileRepository.html
.. _sftp factory: ../../api/sbt/Resolver$$Define.html
.. _SftpRepository API: ../../api/sbt/SftpRepository.html
.. _Ivy ssh: http://ant.apache.org/ivy/history/latest-milestone/resolver/ssh.html
.. _ssh factory: ../../api/sbt/Resolver$$Define.html
.. _SshRepository API: ../../api/sbt/SshRepository.html
.. _Ivy url: http://ant.apache.org/ivy/history/latest-milestone/resolver/url.html
.. _url factory: ../../api/sbt/Resolver$$url$.html
.. _URLRepository API: ../../api/sbt/URLRepository.html

========== ================= ================= =====================  =====================
Type       Factory           Ivy Docs          Factory API            Repository Class API
========== ================= ================= =====================  =====================
Filesystem ``Resolver.file`` `Ivy filesystem`_ `filesystem factory`_  `FileRepository API`_
SFTP       ``Resolver.sftp`` `Ivy sftp`_       `sftp factory`_        `SftpRepository API`_
SSH        ``Resolver.ssh``  `Ivy ssh`_        `ssh factory`_         `SshRepository API`_
URL        ``Resolver.url``  `Ivy url`_        `url factory`_         `URLRepository API`_
========== ================= ================= =====================  =====================

Basic Examples
~~~~~~~~~~~~~~

These are basic examples that use the default Maven-style repository
layout.

Filesystem
^^^^^^^^^^

Define a filesystem repository in the ``test`` directory of the current
working directory and declare that publishing to this repository must be
atomic.

::

    resolvers += Resolver.file("my-test-repo", file("test")) transactional()

URL
^^^

Define a URL repository at ``"http://example.org/repo-releases/"``.

::

    resolvers += Resolver.url("my-test-repo", url("http://example.org/repo-releases/"))

To specify an Ivy repository, use:

::

    resolvers += Resolver.url("my-test-repo", url)(Resolver.ivyStylePatterns)

or customize the layout pattern described in the Custom Layout section
below.

SFTP and SSH Repositories
^^^^^^^^^^^^^^^^^^^^^^^^^

The following defines a repository that is served by SFTP from host
``"example.org"``:

::

    resolvers += Resolver.sftp("my-sftp-repo", "example.org")

To explicitly specify the port:

::

    resolvers += Resolver.sftp("my-sftp-repo", "example.org", 22)

To specify a base path:

::

    resolvers += Resolver.sftp("my-sftp-repo", "example.org", "maven2/repo-releases/")

Authentication for the repositories returned by ``sftp`` and ``ssh`` can
be configured by the ``as`` methods.

To use password authentication:

::

    resolvers += Resolver.ssh("my-ssh-repo", "example.org") as("user", "password")

or to be prompted for the password:

::

    resolvers += Resolver.ssh("my-ssh-repo", "example.org") as("user")

To use key authentication:

::

    resolvers += {
      val keyFile: File = ...
      Resolver.ssh("my-ssh-repo", "example.org") as("user", keyFile, "keyFilePassword")
    }

or if no keyfile password is required or if you want to be prompted for
it:

::

    resolvers += Resolver.ssh("my-ssh-repo", "example.org") as("user", keyFile)

To specify the permissions used when publishing to the server:

::

    resolvers += Resolver.ssh("my-ssh-repo", "example.org") withPermissions("0644")

This is a chmod-like mode specification.

Custom Layout
~~~~~~~~~~~~~

These examples specify custom repository layouts using patterns. The
factory methods accept an ``Patterns`` instance that defines the
patterns to use. The patterns are first resolved against the base file
or URL. The default patterns give the default Maven-style layout.
Provide a different Patterns object to use a different layout. For
example:

::

    resolvers += Resolver.url("my-test-repo", url)( Patterns("[organisation]/[module]/[revision]/[artifact].[ext]") )

You can specify multiple patterns or patterns for the metadata and
artifacts separately. You can also specify whether the repository should
be Maven compatible (as defined by Ivy). See the `patterns
API <../../api/sbt/Patterns$.html>`_ for
the methods to use.

For filesystem and URL repositories, you can specify absolute patterns
by omitting the base URL, passing an empty ``Patterns`` instance, and
using ``ivys`` and ``artifacts``:

::

    resolvers += Resolver.url("my-test-repo") artifacts
            "http://example.org/[organisation]/[module]/[revision]/[artifact].[ext]"


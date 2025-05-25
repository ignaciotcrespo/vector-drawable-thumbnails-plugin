package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.interfaces.ItemSorter
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit

class VectorsPresenterTest {

    private lateinit var presenter: VectorsPresenter
    private lateinit var mockFileProvider: VectorFileProvider
    private lateinit var mockAttributeParser: VectorAttributeParser
    private lateinit var mockImageRenderer: VectorImageRenderer
    private lateinit var mockItemFilter: ItemFilter<VectorItem>
    private lateinit var mockItemSorter: ItemSorter<VectorItem>
    private lateinit var mockProject: Project
    private lateinit var testIoScheduler: TestScheduler
    private lateinit var testProcessingScheduler: TestScheduler
    private lateinit var testPresenterEventsObserver: TestObserver<PresenterEvent>

    // Mocks for data objects
    private lateinit var mockValidFile1: ValidFile
    private lateinit var mockValidFile2: ValidFile
    private lateinit var mockFile1: File
    private lateinit var mockFile2: File
    private lateinit var mockParsedAttributes1: ParsedVectorAttributes
    private lateinit var mockParsedAttributes2: ParsedVectorAttributes
    private lateinit var mockImage1: BufferedImage
    private lateinit var mockImage2: BufferedImage
    private lateinit var mockVectorItem1: VectorItem
    private lateinit var mockVectorItem2: VectorItem

    @BeforeEach
    fun setUp() {
        mockFileProvider = mock()
        mockAttributeParser = mock()
        mockImageRenderer = mock()
        mockItemFilter = mock()
        mockItemSorter = mock()
        mockProject = mock()

        testIoScheduler = TestScheduler()
        testProcessingScheduler = TestScheduler()

        presenter = VectorsPresenter(
            mockFileProvider,
            mockAttributeParser,
            mockImageRenderer,
            mockItemFilter,
            mockItemSorter,
            testIoScheduler,
            testProcessingScheduler
        )

        testPresenterEventsObserver = TestObserver()
        presenter.presenterEvents.subscribe(testPresenterEventsObserver)

        // Setup mock data
        mockFile1 = mock()
        whenever(mockFile1.name).thenReturn("vector1.xml")
        whenever(mockFile1.length()).thenReturn(100L)
        mockValidFile1 = ValidFile(mockFile1, "/path/to/vector1.xml")

        mockFile2 = mock()
        whenever(mockFile2.name).thenReturn("vector2.xml")
        whenever(mockFile2.length()).thenReturn(200L)
        mockValidFile2 = ValidFile(mockFile2, "/path/to/vector2.xml")

        mockParsedAttributes1 = mock()
        whenever(mockParsedAttributes1.name).thenReturn("vector1.xml")
        whenever(mockParsedAttributes1.viewportW).thenReturn(24)
        whenever(mockParsedAttributes1.viewportH).thenReturn(24)
        whenever(mockParsedAttributes1.fileSize).thenReturn(100L)
        whenever(mockParsedAttributes1.validFile).thenReturn(mockValidFile1)


        mockParsedAttributes2 = mock()
        whenever(mockParsedAttributes2.name).thenReturn("vector2.xml")
        whenever(mockParsedAttributes2.viewportW).thenReturn(32)
        whenever(mockParsedAttributes2.viewportH).thenReturn(32)
        whenever(mockParsedAttributes2.fileSize).thenReturn(200L)
        whenever(mockParsedAttributes2.validFile).thenReturn(mockValidFile2)

        mockImage1 = mock()
        mockImage2 = mock()

        mockVectorItem1 = VectorItem("vector1.xml", mockImage1, mockValidFile1, 24, 24, 100L)
        mockVectorItem2 = VectorItem("vector2.xml", mockImage2, mockValidFile2, 32, 32, 200L)

        // Mock FileInputStream for getItemsObservable - this is a common issue with static/final classes in Java
        // For a real-world scenario, this part of getItemsObservable would be refactored to be more testable,
        // e.g., by injecting a stream provider. For now, we assume this part works or test around it.
        // We can mock the attributeParser and imageRenderer calls which are the core logic.
    }

    @AfterEach
    fun tearDown() {
        testPresenterEventsObserver.dispose()
    }

    @Test
    fun `refreshPropertiesData triggers refresh and emits SEARCHING then IDLE with items`() {
        whenever(mockFileProvider.getValidFilesObservable(mockProject)).thenReturn(Observable.just(mockValidFile1, mockValidFile2))
        whenever(mockAttributeParser.parse(any(), eq("vector1.xml"), eq(100L), eq(mockValidFile1))).thenReturn(mockParsedAttributes1)
        whenever(mockAttributeParser.parse(any(), eq("vector2.xml"), eq(200L), eq(mockValidFile2))).thenReturn(mockParsedAttributes2)
        whenever(mockImageRenderer.render(mockParsedAttributes1)).thenReturn(mockImage1)
        whenever(mockImageRenderer.render(mockParsedAttributes2)).thenReturn(mockImage2)

        presenter.refreshPropertiesData(mockProject, delay = false) // delay = false to avoid TestScheduler time advance for delay

        testPresenterEventsObserver.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }

        testIoScheduler.triggerActions()
        testProcessingScheduler.triggerActions() // For flatMap and subsequent operations

        testPresenterEventsObserver.assertValueCount(4) // SEARCHING, VectorFound1, VectorFound2, IDLE
        testPresenterEventsObserver.assertValueAt(1) { it is VectorFoundPresenterEvent && it.item.name == "vector1.xml" }
        testPresenterEventsObserver.assertValueAt(2) { it is VectorFoundPresenterEvent && it.item.name == "vector2.xml" }
        testPresenterEventsObserver.assertValueAt(3) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }

        assertEquals(2, presenter.itemsFiltered().size) // Check items list in presenter
    }

    @Test
    fun `refreshPropertiesData with delay`() {
        whenever(mockFileProvider.getValidFilesObservable(mockProject)).thenReturn(Observable.empty())
        presenter.refreshPropertiesData(mockProject, delay = true)

        testPresenterEventsObserver.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }
        assertEquals(0, presenter.itemsFiltered().size) // No items yet

        testIoScheduler.advanceTimeBy(1, TimeUnit.SECONDS) // Not enough for 2s delay
        testProcessingScheduler.triggerActions()
        assertEquals(0, presenter.itemsFiltered().size)
        testPresenterEventsObserver.assertValueCount(1) // Still just SEARCHING

        testIoScheduler.advanceTimeBy(1, TimeUnit.SECONDS) // Total 2s delay passed on ioScheduler where delay runs
        testProcessingScheduler.triggerActions() // Trigger processing after delay

        testPresenterEventsObserver.assertValueCount(2) // SEARCHING, IDLE
        testPresenterEventsObserver.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
    }


    @Test
    fun `refresh when fileProvider emits error`() {
        val runtimeException = RuntimeException("File provider error")
        whenever(mockFileProvider.getValidFilesObservable(mockProject)).thenReturn(Observable.error(runtimeException))

        presenter.refreshPropertiesData(mockProject, delay = false)

        testPresenterEventsObserver.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }

        testIoScheduler.triggerActions()
        testProcessingScheduler.triggerActions()

        testPresenterEventsObserver.assertValueCount(2) // SEARCHING, IDLE
        testPresenterEventsObserver.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assertEquals(0, presenter.itemsFiltered().size)
    }

    @Test
    fun `refresh when attributeParser returns null`() {
        whenever(mockFileProvider.getValidFilesObservable(mockProject)).thenReturn(Observable.just(mockValidFile1))
        whenever(mockAttributeParser.parse(any(), any(), any(), any())).thenReturn(null) // Parser returns null

        presenter.refreshPropertiesData(mockProject, delay = false)
        testIoScheduler.triggerActions()
        testProcessingScheduler.triggerActions()

        testPresenterEventsObserver.assertValueCount(2) // SEARCHING, IDLE
        testPresenterEventsObserver.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assertEquals(0, presenter.itemsFiltered().size) // No item added
    }

    @Test
    fun `refresh when imageRenderer returns null`() {
        whenever(mockFileProvider.getValidFilesObservable(mockProject)).thenReturn(Observable.just(mockValidFile1))
        whenever(mockAttributeParser.parse(any(), any(), any(), any())).thenReturn(mockParsedAttributes1)
        whenever(mockImageRenderer.render(any())).thenReturn(null) // Renderer returns null

        presenter.refreshPropertiesData(mockProject, delay = false)
        testIoScheduler.triggerActions()
        testProcessingScheduler.triggerActions()

        testPresenterEventsObserver.assertValueCount(2) // SEARCHING, IDLE
        testPresenterEventsObserver.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        assertEquals(0, presenter.itemsFiltered().size) // No item added
    }


    @Test
    fun `setState emits event only when state changes`() {
        // Initial state is IDLE, set by init or previous tests if not careful with instance reuse
        // Let's assume presenter is freshly created, initial state is IDLE.
        // First call to setState to SEARCHING (different from initial IDLE implicit in constructor)
        (presenter as Any).javaClass.getDeclaredMethod("setState", VectorStatePresenterEvent.State::class.java).let {
            it.isAccessible = true
            it.invoke(presenter, VectorStatePresenterEvent.State.SEARCHING)
        }
        testPresenterEventsObserver.assertValueAt(0) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.SEARCHING }

        // Call again with SEARCHING
        (presenter as Any).javaClass.getDeclaredMethod("setState", VectorStatePresenterEvent.State::class.java).let {
            it.isAccessible = true
            it.invoke(presenter, VectorStatePresenterEvent.State.SEARCHING)
        }
        testPresenterEventsObserver.assertValueCount(1) // No new event

        // Call with IDLE
        (presenter as Any).javaClass.getDeclaredMethod("setState", VectorStatePresenterEvent.State::class.java).let {
            it.isAccessible = true
            it.invoke(presenter, VectorStatePresenterEvent.State.IDLE)
        }
        testPresenterEventsObserver.assertValueAt(1) { it is VectorStatePresenterEvent && it.state == VectorStatePresenterEvent.State.IDLE }
        testPresenterEventsObserver.assertValueCount(2)
    }

    @Test
    fun `onVectorClicked triggers Utils_openValidFile`() {
        // Mocking static Utils.openValidFile
        mockStatic(Utils::class.java).use { utilsMock ->
            presenter.onVectorClicked(mockProject, mockVectorItem1)
            testIoScheduler.triggerActions() // For the uiEvents subscription
            utilsMock.verify { Utils.openValidFile(mockProject, mockValidFile1) }
        }
    }

    @Test
    fun `filter updates filterText`() {
        presenter.filter("testQuery")
        // Access private field via reflection for verification if no public getter
        val filterText = (presenter as Any).javaClass.getDeclaredField("filterText").let {
            it.isAccessible = true
            it.get(presenter)
        }
        assertEquals("testquery", filterText) // Should be lowercased
    }

    @Test
    fun `sortBy2 updates sort property`() {
        presenter.sortBy2("By Width")
        val sortProp = (presenter as Any).javaClass.getDeclaredField("sort").let {
            it.isAccessible = true
            it.get(presenter)
        }
        assertEquals("By Width", sortProp)
    }

    @Test
    fun `sortByDirection updates sortDirection property`() {
        presenter.sortByDirection("Desc")
        val sortDir = (presenter as Any).javaClass.getDeclaredField("sortDirection").let {
            it.isAccessible = true
            it.get(presenter)
        }
        assertEquals("Desc", sortDir)
    }
}
